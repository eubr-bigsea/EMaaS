package genericEntity.util.data.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import genericEntity.datasource.DataSource;
import genericEntity.preprocessor.Preprocessor;
import genericEntity.util.AbstractCleanable;
import genericEntity.util.data.GenericObject;

public class StorageManager extends AbstractCleanable {
	
	private final Collection<DataSource> dataSources = new ArrayList<DataSource>();
	private boolean inMemoryProcessingEnabled = false;
	private transient boolean dataExtracted = false;
	private GenericObjectStorage<GenericObject> data;
	private final Collection<Preprocessor> defaultPreprocessors = new ArrayList<Preprocessor>();
	private Vector<GenericObject> objects = new Vector<GenericObject>();
	private final Map<DataSource, Collection<Preprocessor>> preprocessors = new HashMap<DataSource, Collection<Preprocessor>>();
	
	
	public void addPreprocessor(Preprocessor preprocessor) {
		if (preprocessor != null) {
			this.defaultPreprocessors.add(preprocessor);
		}
	}

	public void addPreprocessor(DataSource source, Preprocessor preprocessor) {
		if (source == null) {
			this.addPreprocessor(preprocessor);
			return;
		}

		if (this.preprocessors.containsKey(source)) {
			this.preprocessors.get(source).add(preprocessor);
		} else {
			Collection<Preprocessor> newCollection = new ArrayList<Preprocessor>();
			newCollection.add(preprocessor);
			this.preprocessors.put(source, newCollection);
		}
	}

	public Vector<GenericObject> getExtractedData(){
		return this.objects;
	}
	
	public void enableInMemoryProcessing() {
		this.inMemoryProcessingEnabled = true;
	}

	public void disableInMemoryProcessing() {
		this.inMemoryProcessingEnabled = false;
	}
	
	public boolean inMemoryProcessingEnabled() {
		return this.inMemoryProcessingEnabled;
	}
	
	public void addDataSource(DataSource source) {
		if (source == null) {
			throw new NullPointerException();
		}

		this.registerCleanable(source);
		this.dataSources.add(source);

		// if a new DataSource is added, the extraction process has to be redone
		this.forceExtraction();
	}
	
	public void forceExtraction() {
		this.dataExtracted = false;
	}
	
	public int getDataSize() {
		if (this.data == null) {
			return 0;
		}

		return this.data.size();
	}
	
	public void extractData() {
		try {
			this.data = this.createStorage("tmp");
		} catch (IOException e) {
			throw new IllegalStateException("IOException occurred while initializing the internal storage.", e);
		}

		JsonableWriter<GenericObject> writer = null;
		try {
			writer = this.data.getWriter();
		} catch (IOException e) {
			throw new IllegalStateException("An IOException occurred while instantiating a writer on the storage.", e);
		}

		for (DataSource source : this.dataSources) {
			for (GenericObject object : source) {
				try {
					this.analyzeGenericObject(object);
					writer.add(object);
				} catch (IOException e) {
					throw new IllegalStateException("An IOException occurred while extracting a record out of '" + source.getIdentifier() + "'.", e);
				}
			}
		}

		try {
			writer.close();
		} catch (IOException e) {
			throw new IllegalStateException("An IOException occurred while closing the storage.", e);
		}
	}
		
		protected GenericObjectStorage<GenericObject> createStorage(String name) throws IOException {
			if (name == null) {
				throw new NullPointerException();
			}

			if (this.inMemoryProcessingEnabled()) {
				return new InMemoryStorage<GenericObject>();
			}

			return new FileBasedStorage<GenericObject>(GenericObject.class, name);
		}
		
		protected void analyzeGenericObject(GenericObject object) {
			// default preprocessors
			for (Preprocessor preprocessor : this.defaultPreprocessors) {
				preprocessor.analyzeGenericObject(object);
			}
			this.objects.add(object);
			// DataSource-related preprocessors
			for (Map.Entry<DataSource, Collection<Preprocessor>> entry : this.preprocessors.entrySet()) {
				if (entry.getKey().getIdentifier().equals(object.getSourceId())) {
					for (Preprocessor preprocessor : entry.getValue()) {
						preprocessor.analyzeGenericObject(object);
					}
				}
			}
		}
		
		public boolean isDataExtracted() {
			return dataExtracted;
		}
	
}
