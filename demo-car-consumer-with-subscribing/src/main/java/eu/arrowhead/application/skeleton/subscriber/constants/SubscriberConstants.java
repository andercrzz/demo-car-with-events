package eu.arrowhead.application.skeleton.subscriber.constants;

public class SubscriberConstants {
	//=================================================================================================
	// members

	public static final String NOTIFICATION_QUEUE = "notifications";
	public static final String PUBLISHER_MY_CUSTOM_EVENT_NOTIFICATION_URI = "/" + "customevent";
	public static final String REQUEST_RECEIVED_NOTIFICATION_URI = "/" + "requestreceived";
	public static final String PRESET_EVENT_TYPES = "preset_events";
	public static final String $PRESET_EVENT_TYPES_WD = "${" + PRESET_EVENT_TYPES + ":" + SubscriberDefaults.DEFAULT_PRESET_EVENT_TYPES + "}";
	public static final String PUBLISHER_MY_CUSTOM_EVENT_TYPE = "MY_CUSTOM_EVENT";
	public static final String REQUEST_RECEIVED_EVENT_TYPE = "REQUEST_RECEIVED";
	
	public static final String CONSUMER_TASK = "consumertask";
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SubscriberConstants() {
		throw new UnsupportedOperationException();
	}
}
