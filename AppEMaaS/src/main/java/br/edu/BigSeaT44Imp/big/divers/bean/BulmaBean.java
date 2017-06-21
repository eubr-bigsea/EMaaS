package br.edu.BigSeaT44Imp.big.divers.bean;

import javax.annotation.PostConstruct; 
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.event.map.OverlaySelectEvent;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Polyline;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Lazy
@Scope("session")
@ViewScoped
@ManagedBean
public class BulmaBean implements Serializable {

	private static final long serialVersionUID = -4936517293604211787L;
	private MapModel polylineModel;
	  
    @PostConstruct
    public void init() {
        polylineModel = new DefaultMapModel();
          
        //Shared coordinates
        LatLng coord1 = new LatLng(36.879466, 30.667648);
        LatLng coord2 = new LatLng(36.883707, 30.689216);
        LatLng coord3 = new LatLng(36.879703, 30.706707);
        LatLng coord4 = new LatLng(36.885233, 30.702323);
      
        //Polyline
        Polyline polyline = new Polyline();
        polyline.getPaths().add(coord1);
        polyline.getPaths().add(coord2);
        polyline.getPaths().add(coord3);
        polyline.getPaths().add(coord4);
          
        polyline.setStrokeWeight(10);
        polyline.setStrokeColor("#FF9900");
        polyline.setStrokeOpacity(0.7);
          
        polylineModel.addOverlay(polyline);
    }
  
    public MapModel getPolylineModel() {
        return polylineModel;
    }
  
    public void onPolylineSelect(OverlaySelectEvent event) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Polyline Selected", null));
    }
    
    public void showPanel(){
    	RequestContext context = RequestContext.getCurrentInstance();
    	
    	context.execute("jQuery('#pnlContentFile').hide()");
 	    context.execute("jQuery('#pnlContentJar').hide()");
	    context.execute("jQuery('#pnlContentBulma').show()");
    }  
}
