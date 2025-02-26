package ai.aitia.demo.car_consumer_with_subscribing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

import ai.aitia.arrowhead.application.library.ArrowheadService;
import ai.aitia.arrowhead.application.library.util.ApplicationCommonConstants;
import ai.aitia.demo.car_common.dto.RegisteredSystemRequestDTO;
import eu.arrowhead.application.skeleton.subscriber.SubscriberUtilities;
import eu.arrowhead.application.skeleton.subscriber.constants.SubscriberConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.common.dto.shared.OrchestrationFlags.Flag;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceInterfaceResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.SubscriptionRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.common.exception.InvalidParameterException;


public class SystemConsumerWithSubscriptionTask extends Thread {
	//=================================================================================================
	// members
	
	private boolean interrupted = false;
	
	private final Logger logger = LogManager.getLogger(SystemConsumerWithSubscriptionTask.class);
	
	@Resource( name = SubscriberConstants.NOTIFICATION_QUEUE )
	private ConcurrentLinkedQueue<EventDTO> notificatonQueue;
	
    @Autowired
	private ArrowheadService arrowheadService;
    
    @Autowired
	protected SSLProperties sslProperties;
	
	@Value(ApplicationCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
	private boolean tokenSecurityFilterEnabled;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SYSTEM_NAME)
	private String applicationSystemName;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_ADDRESS_WD)
	private String applicationSystemAddress;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_PORT_WD)
	private int applicationSystemPort;
	
	@Value(SystemConsumerConstants.$REORCHESTRATION_WD)
	private boolean reorchestration;
	
	@Value(SystemConsumerConstants.$MAX_RETRY_WD)
	private int max_retry;

	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------	
	@Override
	public void run() {
		logger.info("ConsumerTask.run started...");
		
		interrupted = Thread.currentThread().isInterrupted();

		OrchestrationResultDTO systemCreationService = null;
		OrchestrationResultDTO systemRequestingService = null;
		
		int counter = 0;
		while (!interrupted && (counter < max_retry)) {
			try {
				if (notificatonQueue.peek() != null) {
					for (final EventDTO event : notificatonQueue) {
						if (SubscriberConstants.PUBLISHER_DESTROYED_EVENT_TYPE.equalsIgnoreCase(event.getEventType())) {
							/*if (reorchestration) {
								logger.info("Recieved publisher custom event - started reorchestration.");
								
								systemCreationService = orchestrateCreateSystemService();
								systemRequestingService = orchestrateGetSystemService();
							} else {
								logger.info("Recieved publisher custom event - started shuting down.");
								System.exit(0);
							}*/

							logger.error("Recieved publisher custom event.");

							String[] parts = event.getPayload().split("/", 2);
							if (parts.length == 2) {
								String name = parts[0];
								String endpoint = parts[1];
							
								// Usar name y endpoint
								logger.error("Name: " + name);
								logger.error("Endpoint: " + endpoint);

								String json = String.format("{"
									+ "\"idShort\": \"%s\","
									+ "\"identification\": {"
									+ "\"id\": \"%sID\","
									+ "\"idType\": \"Custom\""
									+ "},"
									+ "\"endpoints\": ["
									+ "{"
									+ "\"type\": \"http\","
									+ "\"address\": \"%s\""
									+ "}"
									+ "],"
									+ "\"submodels\": [{"
									+ "\"idShort\": \"%sSubmodel\","
									+ "\"identification\": {"
									+ "\"id\": \"%sSubmodelID\""
									+ "},"
									+ "\"endpoints\": ["
									+ "{"
									+ "\"type\": \"http\","
									+ "\"address\": \"%s/aas/submodels/%sSubmodel\""
									+ "}]"
									+ "}]"
									+ "}", name, name, endpoint, name, name, endpoint, name);

								HttpClient client = HttpClient.newHttpClient();
								HttpRequest request = HttpRequest.newBuilder()
									.uri(URI.create("http://localhost:8082/registry/api/v1/registry/"+name+"ID"))
									.header("Content-Type", "application/json")
									.PUT(BodyPublishers.ofString(json))
									.build();

								client.sendAsync(request, BodyHandlers.ofString())
									.thenApply(HttpResponse::body)
									.thenAccept(System.out::println)
									.join();

									logger.info("New system registered in Basyx.");

							} else {
								logger.error("Invalid payload format");
							}
	
							//=================================================================================
						} else {
							logger.info("ConsumerTask recieved event - with type: " + event.getEventType() + ", and payload: " + event.getPayload() + ".");
						}
					}
					
					notificatonQueue.clear();
				}
					
				if (systemCreationService != null  && systemRequestingService != null) {
					// Lista de sistemas nuevos a crear
					final List<RegisteredSystemRequestDTO> systemsToCreate = List.of(new RegisteredSystemRequestDTO("MyAsset", "http://localhost:5080"), new RegisteredSystemRequestDTO("MyAsset2", "http://localhost:5081"), new RegisteredSystemRequestDTO("MyAsset3", "http://localhost:5082"), new RegisteredSystemRequestDTO("MyAsset4", "http://localhost:5083"));
					// callSystemCreationService(systemCreationService , systemsToCreate);
					// callSystemRequestingService(systemRequestingService);
				} else {
					counter++;
					
					systemCreationService = orchestrateCreateSystemService();
					systemRequestingService = orchestrateGetSystemService();
					
					if (systemCreationService != null  && systemRequestingService != null) {
						counter = 0;
						
						final Set<SystemResponseDTO> sources = new HashSet<SystemResponseDTO>();
						
						sources.add(systemCreationService.getProvider());
						sources.add(systemRequestingService.getProvider());
						
						subscribeToDestoryEvents(sources);
					}
				}
			} catch (final Throwable ex) {
				logger.debug(ex.getMessage());
				
				systemCreationService = null;
				systemRequestingService = null;
			}	
		}
		
		System.exit(0);
	}
	
	//-------------------------------------------------------------------------------------------------	
	public void destroy() {
		logger.debug("ConsumerTask.destroy started...");
		
		interrupted = true;
	}
	
	//=================================================================================================
	//Assistant methods

    //-------------------------------------------------------------------------------------------------
    private void callSystemCreationService(final OrchestrationResultDTO orchestrationResult, final List<RegisteredSystemRequestDTO> systemsToCreate) {
    	logger.debug("consumeCreateCarService started...");
    	
		validateOrchestrationResult(orchestrationResult, SystemConsumerConstants.CREATE_SYSTEM_SERVICE_DEFINITION);
			
		for (final RegisteredSystemRequestDTO systemRequestDTO : systemsToCreate) {
			logger.info("Create a system request:");
			printOut(systemRequestDTO);
			final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());
			final SystemResponseDTO systemCreated = arrowheadService.consumeServiceHTTP(SystemResponseDTO.class, HttpMethod.valueOf(orchestrationResult.getMetadata().get(SystemConsumerConstants.HTTP_METHOD)),
					orchestrationResult.getProvider().getAddress(), orchestrationResult.getProvider().getPort(), orchestrationResult.getServiceUri(),
					getInterface(), token, systemRequestDTO, new String[0]);
			logger.info("Provider response");
			printOut(systemCreated);
		}			
    }
	
	//-------------------------------------------------------------------------------------------------
	private void subscribeToDestoryEvents(final Set<SystemResponseDTO> providers) {
		final Set<SystemRequestDTO> sources = new HashSet<>(providers.size());
		
		for (final SystemResponseDTO provider : providers) {
			final SystemRequestDTO source = new SystemRequestDTO();
			source.setSystemName(provider.getSystemName());
			source.setAddress(provider.getAddress());
			source.setPort(provider.getPort());
			
			sources.add(source);
		}
		
		final SystemRequestDTO subscriber = new SystemRequestDTO();
		subscriber.setSystemName(applicationSystemName);
		subscriber.setAddress(applicationSystemAddress);
		subscriber.setPort(applicationSystemPort);
		
		if (sslEnabled) {
			subscriber.setAuthenticationInfo(Base64.getEncoder().encodeToString( arrowheadService.getMyPublicKey().getEncoded()));		
		}
		
		try {
			arrowheadService.unsubscribeFromEventHandler(SubscriberConstants.PUBLISHER_DESTROYED_EVENT_TYPE, applicationSystemName, applicationSystemAddress, applicationSystemPort);
		} catch (final Exception ex) {
			logger.debug("Exception happend in subscription initalization " + ex);
		}
		
		try {
			final SubscriptionRequestDTO subscription = SubscriberUtilities.createSubscriptionRequestDTO(SubscriberConstants.PUBLISHER_DESTROYED_EVENT_TYPE, subscriber, SubscriberConstants.PUBLISHER_DESTORYED_NOTIFICATION_URI);
			subscription.setSources(sources);
			
			arrowheadService.subscribeToEventHandler(subscription);
		} catch (final InvalidParameterException ex) {
			
			if (ex.getMessage().contains( "Subscription violates uniqueConstraint rules")) {
				logger.debug("Subscription is already in DB");
			} else {
				logger.debug(ex.getMessage());
				logger.debug(ex);
			}
		} catch (final Exception ex) {
			logger.debug("Could not subscribe to EventType: " + SubscriberConstants.PUBLISHER_DESTROYED_EVENT_TYPE);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
    private OrchestrationResultDTO orchestrateCreateSystemService() {
    	logger.info("Orchestration request for " + SystemConsumerConstants.CREATE_SYSTEM_SERVICE_DEFINITION + " service:");
    	final ServiceQueryFormDTO serviceQueryForm = new ServiceQueryFormDTO.Builder(SystemConsumerConstants.CREATE_SYSTEM_SERVICE_DEFINITION)
    																		.interfaces(getInterface())
    																		.build();
    	
		final OrchestrationFormRequestDTO.Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		final OrchestrationFormRequestDTO orchestrationFormRequest = orchestrationFormBuilder.requestedService(serviceQueryForm)
																					   .flag(Flag.MATCHMAKING, false)
																					   .flag(Flag.OVERRIDE_STORE, true)
																					   .flag(Flag.PING_PROVIDERS, true)
																					   .build();
		
		printOut(orchestrationFormRequest);		
		
		final OrchestrationResponseDTO orchestrationResponse = arrowheadService.proceedOrchestration(orchestrationFormRequest);
		logger.info("Orchestration response:");
		printOut(orchestrationResponse);	
		
		if (orchestrationResponse == null) {
			logger.info("No orchestration response received");
		} else if (orchestrationResponse.getResponse().isEmpty()) {
			logger.info("No provider found during the orchestration");
		} else {
			final OrchestrationResultDTO orchestrationResult = orchestrationResponse.getResponse().get(0);
			validateOrchestrationResult(orchestrationResult, SystemConsumerConstants.CREATE_SYSTEM_SERVICE_DEFINITION);
			
			return orchestrationResult;
			
		}
		
		return null;
    }
    
    //-------------------------------------------------------------------------------------------------
    private OrchestrationResultDTO orchestrateGetSystemService() {
    	logger.info("Orchestration request for " + SystemConsumerConstants.GET_SYSTEM_SERVICE_DEFINITION + " service:");
    	final ServiceQueryFormDTO serviceQueryForm = new ServiceQueryFormDTO.Builder(SystemConsumerConstants.GET_SYSTEM_SERVICE_DEFINITION)
    																		.interfaces(getInterface())
    																		.build();
    	
		final OrchestrationFormRequestDTO.Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		final OrchestrationFormRequestDTO orchestrationFormRequest = orchestrationFormBuilder.requestedService(serviceQueryForm)
																					   .flag(Flag.MATCHMAKING, false)
																					   .flag(Flag.OVERRIDE_STORE, true)
																					   .flag(Flag.PING_PROVIDERS, true)
																					   .build();
		
		printOut(orchestrationFormRequest);		
		
		final OrchestrationResponseDTO orchestrationResponse = arrowheadService.proceedOrchestration(orchestrationFormRequest);
		
		logger.info("Orchestration response:");
		printOut(orchestrationResponse);		
		
		if (orchestrationResponse == null) {
			logger.info("No orchestration response received");
		} else if (orchestrationResponse.getResponse().isEmpty()) {
			logger.info("No provider found during the orchestration");
		} else {
			final OrchestrationResultDTO orchestrationResult = orchestrationResponse.getResponse().get(0);
			validateOrchestrationResult(orchestrationResult, SystemConsumerConstants.GET_SYSTEM_SERVICE_DEFINITION);
			
			return orchestrationResult;
		}
		
		return null;
    }
    
    //-------------------------------------------------------------------------------------------------
    private void callSystemRequestingService( final OrchestrationResultDTO orchestrationResult) {
		validateOrchestrationResult(orchestrationResult, SystemConsumerConstants.GET_SYSTEM_SERVICE_DEFINITION);
		
		logger.info("Get all systems:");
		final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());
		@SuppressWarnings("unchecked")
		final List<SystemResponseDTO> allSystems = arrowheadService.consumeServiceHTTP(List.class, HttpMethod.valueOf(orchestrationResult.getMetadata().get(SystemConsumerConstants.HTTP_METHOD)),
																				orchestrationResult.getProvider().getAddress(), orchestrationResult.getProvider().getPort(), orchestrationResult.getServiceUri(),
																				getInterface(), token, null, new String[0]);
		printOut(allSystems);
		
		/*logger.info("Get only blue cars:");
		final String[] queryParamColor = {orchestrationResult.getMetadata().get(CarConsumerConstants.REQUEST_PARAM_KEY_COLOR), "blue"};			
		@SuppressWarnings("unchecked")
		final List<CarResponseDTO> blueCars = arrowheadService.consumeServiceHTTP(List.class, HttpMethod.valueOf(orchestrationResult.getMetadata().get(CarConsumerConstants.HTTP_METHOD)),
																				  orchestrationResult.getProvider().getAddress(), orchestrationResult.getProvider().getPort(), orchestrationResult.getServiceUri(),
																				  getInterface(), token, null, queryParamColor);
		printOut(blueCars);
		*/
    }
    
    //-------------------------------------------------------------------------------------------------
    private String getInterface() {
    	return sslProperties.isSslEnabled() ? SystemConsumerConstants.INTERFACE_SECURE : SystemConsumerConstants.INTERFACE_INSECURE;
    }
    
    //-------------------------------------------------------------------------------------------------
    private void validateOrchestrationResult(final OrchestrationResultDTO orchestrationResult, final String serviceDefinition) {
    	if (!orchestrationResult.getService().getServiceDefinition().equalsIgnoreCase(serviceDefinition)) {
			throw new InvalidParameterException("Requested and orchestrated service definition do not match");
		}
    	
    	boolean hasValidInterface = false;
    	for (final ServiceInterfaceResponseDTO serviceInterface : orchestrationResult.getInterfaces()) {
			if (serviceInterface.getInterfaceName().equalsIgnoreCase(getInterface())) {
				hasValidInterface = true;
				break;
			}
		}
    	if (!hasValidInterface) {
    		throw new InvalidParameterException("Requested and orchestrated interface do not match");
		}
    }
    
    //-------------------------------------------------------------------------------------------------
    private void printOut(final Object object) {
    	System.out.println(Utilities.toPrettyJson(Utilities.toJson(object)));
    }
}