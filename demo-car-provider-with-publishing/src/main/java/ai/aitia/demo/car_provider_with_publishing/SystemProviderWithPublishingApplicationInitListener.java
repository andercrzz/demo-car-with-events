package ai.aitia.demo.car_provider_with_publishing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.library.ArrowheadService;
import ai.aitia.arrowhead.application.library.config.ApplicationInitListener;
import ai.aitia.arrowhead.application.library.util.ApplicationCommonConstants;
import eu.arrowhead.application.skeleton.provider.security.ProviderSecurityConfig;
import eu.arrowhead.application.skeleton.publisher.event.PresetEventType;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.dto.shared.EventPublishRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;

@Component
public class SystemProviderWithPublishingApplicationInitListener extends ApplicationInitListener {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ArrowheadService arrowheadService;
	
	@Autowired
	private ProviderSecurityConfig providerSecurityConfig;
	
	@Value(ApplicationCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
	private boolean tokenSecurityFilterEnabled;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SYSTEM_NAME)
	private String mySystemName;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_ADDRESS_WD)
	private String mySystemAddress;
	
	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_PORT_WD)
	private int mySystemPort;
	
	private final Logger logger = LogManager.getLogger(SystemProviderWithPublishingApplicationInitListener.class);
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		checkConfiguration();
		
		//Checking the availability of necessary core systems
		checkCoreSystemReachability(CoreSystem.SERVICEREGISTRY);
		if (sslEnabled && tokenSecurityFilterEnabled) {
			checkCoreSystemReachability(CoreSystem.AUTHORIZATION);			

			//Initialize Arrowhead Context
			arrowheadService.updateCoreServiceURIs(CoreSystem.AUTHORIZATION);
			
			setTokenSecurityFilter();
		} else {
			logger.info("TokenSecurityFilter in not active");
		}		
		
		//Register services into ServiceRegistry
		final ServiceRegistryRequestDTO createCarServiceRequest = createServiceRegistryRequest(SystemProviderWithPublishingConstants.CREATE_SYSTEM_SERVICE_DEFINITION, SystemProviderWithPublishingConstants.SYSTEM_URI, HttpMethod.POST);		
		arrowheadService.forceRegisterServiceToServiceRegistry(createCarServiceRequest);
		
		final ServiceRegistryRequestDTO getCarServiceRequest = createServiceRegistryRequest(SystemProviderWithPublishingConstants.GET_SYSTEM_SERVICE_DEFINITION,  SystemProviderWithPublishingConstants.SYSTEM_URI, HttpMethod.GET);
		getCarServiceRequest.getMetadata().put(SystemProviderWithPublishingConstants.REQUEST_PARAM_KEY_NAME, SystemProviderWithPublishingConstants.REQUEST_PARAM_NAME);
		getCarServiceRequest.getMetadata().put(SystemProviderWithPublishingConstants.REQUEST_PARAM_KEY_ENDPOINT, SystemProviderWithPublishingConstants.REQUEST_PARAM_ENDPOINT);
		arrowheadService.forceRegisterServiceToServiceRegistry(getCarServiceRequest);
		
		if (arrowheadService.echoCoreSystem(CoreSystem.EVENTHANDLER)) {
			arrowheadService.updateCoreServiceURIs(CoreSystem.EVENTHANDLER);	
		}

		// Start a new thread to run publishDestroyedEvent() every 10 seconds
		/*Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				publishMyEvent();
				logger.info("Sending event to event handler");
			}
		}, 0, 10000);
		*/

		// Show menu in console
		showMenu();
	}

	// Method to show menu and handle user input
	private void showMenu() {
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Select an option:");
			System.out.println("1. View assets");
			System.out.println("2. Create new asset");
			System.out.println("3. Exit");

			int choice = scanner.nextInt();
			scanner.nextLine(); // Consume newline

			switch (choice) {
				case 1:
					// Logic to view assets
					System.out.println("Viewing assets...");
					viewAssets();
					break;
				case 2:
					// Logic to create new asset
					System.out.println("Enter asset name:\n");
					String name = scanner.nextLine();
					System.out.println("Enter asset endpoint:\n");
					String endpoint = scanner.nextLine();
					publishMyEvent(name, endpoint);
					break;
				case 3:
					System.out.println("Exiting...");
					scanner.close();
					customDestroy();
					return;
				default:
					System.out.println("Invalid choice. Please try again.");
			}
		}
	}

	// Method to view assets
	private void viewAssets() {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://localhost:8082/registry/api/v1/registry"))
			.GET()
			.build();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(this::parseAndDisplayAssets)
			.join();
	}

	// Method to parse and display assets
	private void parseAndDisplayAssets(String responseBody) {
		JSONArray assets = new JSONArray(responseBody);
		for (int i = 0; i < assets.length(); i++) {
			JSONObject asset = assets.getJSONObject(i);
			String idShort = asset.getString("idShort");
			String id = asset.getJSONObject("identification").getString("id");
			String endpoint = asset.getJSONArray("endpoints").getJSONObject(0).getString("address");

			System.out.println("Asset ID Short: " + idShort);
			System.out.println("Asset ID: " + id);
			System.out.println("Endpoint: " + endpoint);
			System.out.println("-------------------------");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void customDestroy() {
		// Unregister service
		arrowheadService.unregisterServiceFromServiceRegistry(SystemProviderWithPublishingConstants.CREATE_SYSTEM_SERVICE_DEFINITION, SystemProviderWithPublishingConstants.SYSTEM_URI);
		arrowheadService.unregisterServiceFromServiceRegistry(SystemProviderWithPublishingConstants.GET_SYSTEM_SERVICE_DEFINITION, SystemProviderWithPublishingConstants.SYSTEM_URI);
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void checkConfiguration() {
		if (!sslEnabled && tokenSecurityFilterEnabled) {			 
			logger.warn("Contradictory configuration:");
			logger.warn("token.security.filter.enabled=true while server.ssl.enabled=false");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void publishMyEvent(String name, String endpoint) {
		final String eventType = PresetEventType.MY_CUSTOM_EVENT.getEventTypeName();
		
		final SystemRequestDTO source = new SystemRequestDTO();
		source.setSystemName(mySystemName);
		source.setAddress(mySystemAddress);
		source.setPort(mySystemPort);
		if (sslEnabled) {
			source.setAuthenticationInfo(Base64.getEncoder().encodeToString( arrowheadService.getMyPublicKey().getEncoded()));
		}

		final Map<String,String> metadata = null;
		final String payload = name + "/" + endpoint;
		final String timeStamp = Utilities.convertZonedDateTimeToUTCString( ZonedDateTime.now() );
		
		final EventPublishRequestDTO publishRequestDTO = new EventPublishRequestDTO(
				eventType, 
				source, 
				metadata, 
				payload, 
				timeStamp);
		
		arrowheadService.publishToEventHandler(publishRequestDTO);
		logger.error("Event published: " + publishRequestDTO);
	}

	//-------------------------------------------------------------------------------------------------
	private void setTokenSecurityFilter() {
		final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
		if (authorizationPublicKey == null) {
			throw new ArrowheadException("Authorization public key is null");
		}
		
		KeyStore keystore;
		try {
			keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
			keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
		} catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
			throw new ArrowheadException(ex.getMessage());
		}			
		final PrivateKey providerPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());
		
		providerSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
		providerSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(providerPrivateKey);
	}
	
	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryRequestDTO createServiceRegistryRequest(final String serviceDefinition, final String serviceUri, final HttpMethod httpMethod) {
		final ServiceRegistryRequestDTO serviceRegistryRequest = new ServiceRegistryRequestDTO();
		serviceRegistryRequest.setServiceDefinition(serviceDefinition);
		final SystemRequestDTO systemRequest = new SystemRequestDTO();
		systemRequest.setSystemName(mySystemName);
		systemRequest.setAddress(mySystemAddress);
		systemRequest.setPort(mySystemPort);		

		if (sslEnabled && tokenSecurityFilterEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.TOKEN.name());
			serviceRegistryRequest.setInterfaces(List.of(SystemProviderWithPublishingConstants.INTERFACE_SECURE));
		} else if (sslEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE.name());
			serviceRegistryRequest.setInterfaces(List.of(SystemProviderWithPublishingConstants.INTERFACE_SECURE));
		} else {
			serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE.name());
			serviceRegistryRequest.setInterfaces(List.of(SystemProviderWithPublishingConstants.INTERFACE_INSECURE));
		}
		serviceRegistryRequest.setProviderSystem(systemRequest);
		serviceRegistryRequest.setServiceUri(serviceUri);
		serviceRegistryRequest.setMetadata(new HashMap<>());
		serviceRegistryRequest.getMetadata().put(SystemProviderWithPublishingConstants.HTTP_METHOD, httpMethod.name());
		return serviceRegistryRequest;
	}
}