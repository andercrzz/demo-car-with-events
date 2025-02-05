package ai.aitia.demo.car_provider_with_publishing.controller;

import java.util.Map;

import org.springframework.http.HttpMethod;

import ai.aitia.demo.car_provider_with_publishing.SystemProviderWithPublishingConstants;
import eu.arrowhead.application.skeleton.publisher.event.EventTypeConstants;
import eu.arrowhead.application.skeleton.publisher.event.PresetEventType;
import eu.arrowhead.application.skeleton.publisher.service.PublisherService;

public class EventPublisherThread implements Runnable {

    private final PublisherService publisherService;

    public EventPublisherThread(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000); // 10 seconds
                publisherService.publish(PresetEventType.REQUEST_RECEIVED, 
                    Map.of(EventTypeConstants.EVENT_TYPE_REQUEST_RECEIVED_METADATA_REQUEST_TYPE, HttpMethod.GET.name()), 
                    SystemProviderWithPublishingConstants.SYSTEM_URI);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}