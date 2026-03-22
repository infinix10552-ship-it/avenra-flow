//package org.devx.automatedinvoicesystem.Config;
//
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.amqp.support.converter.SimpleMessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitMQConfig {
//
//    // 1. Define the exact names of our routing components
//    public static final String INVOICE_QUEUE = "invoice_queue";
//    public static final String INVOICE_EXCHANGE = "invoice_exchange";
//    public static final String INVOICE_ROUTING_KEY = "invoice.process.routing.key";
//
//    //2.Create the queue
//    @Bean
//    public Queue invoiceQueue() {
//        return new Queue(INVOICE_QUEUE, true);
//    }
//
//    //3. Create the exchange
//    @Bean
//    public DirectExchange invoiceExchange() {
//        return new DirectExchange(INVOICE_EXCHANGE);
//    }
//
//    //4. Binding the queue to exchange via routing key
//    @Bean
//    public Binding binding(Queue invoiceQueue, DirectExchange invoiceExchange) {
//        return BindingBuilder.bind(invoiceQueue).to(invoiceExchange).with(INVOICE_ROUTING_KEY);
//    }
//
//    //5. JSON Message Converter - using SimpleMessageConverter with createMessageIds enabled
//    @Bean
//    public MessageConverter messageConverter() {
//        SimpleMessageConverter converter = new SimpleMessageConverter();
//        converter.setCreateMessageIds(true);
//        return converter;
//    }
//
//
//
//    //6. Configure RabbitTemplate with Message Converter
//    @Bean
//    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
//                                        MessageConverter messageConverter) {
//        RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        // Apply the message converter for proper serialization
//        template.setMessageConverter(messageConverter);
//        template.setDefaultReceiveQueue(INVOICE_QUEUE);
//        // Enable message marshaling to JSON by default
//        template.setExchange(INVOICE_EXCHANGE);
//        template.setRoutingKey(INVOICE_ROUTING_KEY);
//        return template;
//    }
//
//}



package org.devx.automatedinvoicesystem.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVOICE_QUEUE = "invoice_queue";
    public static final String INVOICE_EXCHANGE = "invoice_exchange";
    public static final String INVOICE_ROUTING_KEY = "invoice.process.routing.key";

    @Bean
    public Queue invoiceQueue() {
        return new Queue(INVOICE_QUEUE, true);
    }

    @Bean
    public DirectExchange invoiceExchange() {
        return new DirectExchange(INVOICE_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue invoiceQueue, DirectExchange invoiceExchange) {
        return BindingBuilder.bind(invoiceQueue).to(invoiceExchange).with(INVOICE_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // No custom MessageConverter or RabbitTemplate beans are defined here, as Spring Boot 4.0.2 will automatically configure a RabbitTemplate with a JSON message converter if Jackson is on the classpath. This simplifies our configuration and reduces boilerplate, while still ensuring that our messages are properly serialized to JSON when sent to RabbitMQ.
}