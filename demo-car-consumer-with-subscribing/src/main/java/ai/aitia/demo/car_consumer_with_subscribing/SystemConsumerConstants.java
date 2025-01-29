package ai.aitia.demo.car_consumer_with_subscribing;

public class SystemConsumerConstants {
	
	//=================================================================================================
	// members
	
	public static final String BASE_PACKAGE = "ai.aitia";
	
	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";
	
	public static final String CREATE_SYSTEM_SERVICE_DEFINITION = "create-system";
	public static final String GET_SYSTEM_SERVICE_DEFINITION = "get-system";
	public static final String REQUEST_PARAM_KEY_NAME = "request-param-name";
	public static final String REQUEST_PARAM_KEY_ENDPOINT = "request-param-endpoint";
	public static final String $REORCHESTRATION_WD = "${reorchestration:false}";
	public static final String $MAX_RETRY_WD = "${max_retry:300}";
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SystemConsumerConstants() {
		throw new UnsupportedOperationException();
	}

}
