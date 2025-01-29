package ai.aitia.demo.car_common.dto;

import java.io.Serializable;

public class RegisteredSystemRequestDTO implements Serializable {

	//=================================================================================================
	// members

	private static final long serialVersionUID = -5363562707054976998L;

	private String name;
	private String endpoint;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public RegisteredSystemRequestDTO(final String name, final String endpoint) {
		this.name = name;
		this.endpoint = endpoint;
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	public String getEndpoint() { return endpoint; }

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) { this.name = name; }
	public void setEndpoint(final String endpoint) { this.endpoint = endpoint; }	
}
