package recordLinkageDF.dependencies;

import java.io.IOException;

//CODLINHA,NOMELINHA,CODVEICULO,NUMEROCARTAO,HORAUTILIZACAO,DATAUTILIZACAO,DATANASCIMENTO,SEXO

public class BUSTEFieldsInputTicket {

	private String codLinha;
	private String nomeLinha;
	private String codVeiculo;
	private String numCartao;
	private String horaUtilizacao;
	private String dataUtilizacao;
	private String dataNasc;
	private String sexo;
	private static final int NUMBER_ATTRIBUTES = 8;

	public BUSTEFieldsInputTicket(String params) throws IOException {
		String[] attributesArray = params.split(",");

		if (attributesArray.length != NUMBER_ATTRIBUTES) {
			throw new IOException("The Ticket input file should have " + NUMBER_ATTRIBUTES
					+ " attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(attributesArray[0], attributesArray[1], attributesArray[2], attributesArray[3],
				attributesArray[4], attributesArray[5], attributesArray[6], attributesArray[7]);
	}

	public BUSTEFieldsInputTicket(String codLinha, String nomeLinha, String codVeiculo, String numCartao,
			String horaUtilizacao, String dataUtilizacao, String dataNasc, String sexo) throws IOException {
		if (codLinha == null || nomeLinha == null || codVeiculo == null || numCartao == null || horaUtilizacao == null
				|| dataUtilizacao == null || dataNasc == null || sexo == null) {
			throw new IOException("The Ticket input file should have " + NUMBER_ATTRIBUTES
					+ " attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(codLinha, nomeLinha, codVeiculo, numCartao, horaUtilizacao, dataUtilizacao, dataNasc,
				sexo);
	}

	private void initializeAttributes(String codLinha, String nomeLinha, String codVeiculo, String numCartao,
			String horaUtilizacao, String dataUtilizacao, String dataNasc, String sexo) {
		this.codLinha = codLinha;
		this.nomeLinha = nomeLinha;
		this.codVeiculo = codVeiculo;
		this.numCartao = numCartao;
		this.horaUtilizacao = horaUtilizacao;
		this.dataUtilizacao = dataUtilizacao;
		this.dataNasc = dataNasc;
		this.sexo = sexo;
	}

	public String getCodLinha() {
		return codLinha;
	}

	public void setCodLinha(String codLinha) {
		this.codLinha = codLinha;
	}

	public String getNomeLinha() {
		return nomeLinha;
	}

	public void setNomeLinha(String nomeLinha) {
		this.nomeLinha = nomeLinha;
	}

	public String getCodVeiculo() {
		return codVeiculo;
	}

	public void setCodVeiculo(String codVeiculo) {
		this.codVeiculo = codVeiculo;
	}

	public String getNumCartao() {
		return numCartao;
	}

	public void setNumCartao(String numCartao) {
		this.numCartao = numCartao;
	}

	public String getHoraUtilizacao() {
		return horaUtilizacao;
	}

	public void setHoraUtilizacao(String horaUtilizacao) {
		this.horaUtilizacao = horaUtilizacao;
	}

	public String getDataUtilizacao() {
		return dataUtilizacao;
	}

	public void setDataUtilizacao(String dataUtilizacao) {
		this.dataUtilizacao = dataUtilizacao;
	}

	public String getDataNasc() {
		return dataNasc;
	}

	public void setDataNasc(String dataNasc) {
		this.dataNasc = dataNasc;
	}

	public String getSexo() {
		return sexo;
	}

	public void setSexo(String sexo) {
		this.sexo = sexo;
	}
}