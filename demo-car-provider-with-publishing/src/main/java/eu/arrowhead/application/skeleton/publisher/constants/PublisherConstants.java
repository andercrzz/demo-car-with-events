package eu.arrowhead.application.skeleton.publisher.constants;

public class PublisherConstants {

	//=================================================================================================
	// members

	public static final String START_INIT_EVENT_PAYLOAD= "InitStarted";
	public static final String START_RUN_EVENT_PAYLOAD= "RunStarted";
	public static final String PUBLISHER_MY_CUSTOM_EVENT_PAYLOAD= "MyCustomEvent";
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private PublisherConstants() {
		throw new UnsupportedOperationException();
	}
}
