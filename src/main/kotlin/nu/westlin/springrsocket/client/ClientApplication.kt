package nu.westlin.springrsocket.client

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.client.TcpClientTransport
import nu.westlin.springrsocket.server.MarketData
import nu.westlin.springrsocket.server.MarketDataRequest
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils


@SpringBootApplication
class ClientApplication(private val rSocketRequester: RSocketRequester) {
    fun getStocks() {
/*
        fetchAndPrintStock("foo")
        fetchAndPrintStock("bar")
        fetchAndPrintStock("foobar")
        fetchAndPrintStock("pingu")
*/

        addStock(MarketData("oof", 93))
    }

    private fun addStock(marketData: MarketData) {
        rSocketRequester
            .route("collectMarketData")
            .data(marketData)
            .send()
            .block()

        fetchAndPrintStock(marketData.stock)
    }

    private fun fetchAndPrintStock(stock: String) {
        val result = rSocketRequester
            .route("currentMarketData")
            .data(MarketDataRequest(stock))
            .retrieveMono(MarketData::class.java)
            .block()

        println("result for \"$stock\" = ${result}")
    }
}

@Configuration
class ClientConfiguration {
    @Bean
    fun rSocket(): RSocket {
        return RSocketFactory
            .connect()
            .mimeType(MimeTypeUtils.APPLICATION_JSON_VALUE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .frameDecoder(PayloadDecoder.ZERO_COPY)
            .transport(TcpClientTransport.create(7000))
            .start()
            .block()!!
    }

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    fun rSocketRequester(rSocketStrategies: RSocketStrategies): RSocketRequester {
        return RSocketRequester.builder()
            .rsocketStrategies(rSocketStrategies)
            .connectTcp("localhost", 7000)
            .block()!!
        //return RSocketRequester.wrap(rSocket(), MimeTypeUtils.APPLICATION_JSON, MetadataExtractor.ROUTE_KEY, rSocketStrategies)
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder()
        .main(ClientApplication::class.java)
        .sources(ClientApplication::class.java)
        .profiles("client")
        .run(*args).getBean<ClientApplication>().getStocks()
}
