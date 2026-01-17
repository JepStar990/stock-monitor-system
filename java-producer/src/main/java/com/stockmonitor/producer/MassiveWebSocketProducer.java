// MassiveWebSocketProducer.java
package com.stockmonitor.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MassiveWebSocketProducer {
    private static final String MASSIVE_WS_URL = System.getenv().getOrDefault("MASSIVE_WS_URL", "wss://socket.massive.com/stocks");
    private static final String KAFKA_BOOTSTRAP = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final String KAFKA_TOPIC = System.getenv().getOrDefault("KAFKA_TOPIC", "raw-trades");
    private static final String API_KEY = System.getenv("MASSIVE_API_KEY");
    
    private final KafkaProducer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;
    
    public MassiveWebSocketProducer() {
        this.kafkaProducer = createKafkaProducer();
        this.objectMapper = new ObjectMapper();
    }
    
    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "10");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");
        
        return new KafkaProducer<>(props);
    }
    
    public void start() {
        try {
            WebSocketClient client = new WebSocketClient(new URI(MASSIVE_WS_URL)) {
                
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to Massive.com WebSocket");
                    
                    // Authenticate
                    String authMessage = String.format(
                        "{\"action\":\"auth\",\"params\":\"%s\"}", API_KEY
                    );
                    send(authMessage);
                    
                    // Subscribe to aggregates for AAPL, TSLA, and crypto
                    String subscribeMessage = 
                        "{\"action\":\"subscribe\",\"params\":\"A.AAPL,A.TSLA,A.X:BTCUSD\"}";
                    send(subscribeMessage);
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode json = objectMapper.readTree(message);
                        
                        // Check if it's a data message (not status)
                        if (json.isArray() && json.size() > 0) {
                            for (JsonNode item : json) {
                                if (item.has("ev")) {
                                    String eventType = item.get("ev").asText();
                                    
                                    // Process aggregate (A) or trade (T) events
                                    if (eventType.equals("A") || eventType.equals("T")) {
                                        processTradeData(item);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing message: " + e.getMessage());
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Connection closed: " + reason);
                    // Implement reconnection logic here
                    reconnect();
                }
                
                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket error: " + ex.getMessage());
                }
            };
            
            client.connect();
            
        } catch (Exception e) {
            System.err.println("Failed to start WebSocket client: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processTradeData(JsonNode tradeData) {
        try {
            // Extract key fields
            String symbol = tradeData.get("sym").asText();
            double price = tradeData.has("c") ? tradeData.get("c").asDouble() : 
                          tradeData.get("p").asDouble();
            long volume = tradeData.has("v") ? tradeData.get("v").asLong() : 1;
            long timestamp = tradeData.get("t").asLong();
            
            // Create enriched message
            String enrichedMessage = String.format(
                "{\"symbol\":\"%s\",\"price\":%.2f,\"volume\":%d,\"timestamp\":%d,\"raw\":%s}",
                symbol, price, volume, timestamp, tradeData.toString()
            );
            
            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(
                KAFKA_TOPIC,
                symbol, // Use symbol as key for partitioning
                enrichedMessage
            );
            
            kafkaProducer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        System.err.println("Error sending to Kafka: " + exception.getMessage());
                    } else {
                        System.out.printf("Sent %s to partition %d offset %d%n",
                            symbol, metadata.partition(), metadata.offset());
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error processing trade data: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        if (kafkaProducer != null) {
            kafkaProducer.close(Duration.ofMillis(5000));
        }
    }
    
    public static void main(String[] args) {

         // Load .env file
         Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();
    
        MassiveWebSocketProducer producer = new MassiveWebSocketProducer();
    
        // Validate API key
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("ERROR: MASSIVE_API_KEY not found in environment variables!");
            System.err.println("Please set MASSIVE_API_KEY in your .env file or environment.");
            System.exit(1);
        } 
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down producer...");
            producer.shutdown();
        }));
        
        producer.start();
    }
}
