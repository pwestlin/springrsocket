package nu.westlin.springrsocket.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono


@SpringBootApplication
class ServerApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder()
        .main(ServerApplication::class.java)
        .sources(ServerApplication::class.java)
        .profiles("server")
        .run(*args)
}

@Repository
class MarketDataRepository {
    private val data = mutableListOf(
        MarketData("foo", 25),
        MarketData("bar", 42),
        MarketData("foobar", 167)
    )

    fun getOne(stock: String) = data.firstOrNull { it.stock == stock }
    fun add(marketData: MarketData) {
        data.add(marketData)
    }
}

@Controller
class MarketDataRSocketController(private val marketDataRepository: MarketDataRepository) {

    @MessageMapping("currentMarketData")
    fun currentMarketData(marketDataRequest: MarketDataRequest): Mono<MarketData> {
        return Mono.justOrEmpty(marketDataRepository.getOne(marketDataRequest.stock!!))
    }

    @MessageMapping("collectMarketData")
    fun collectMarketData(marketData: MarketData): Mono<Void?>? {
        marketDataRepository.add(marketData)
        return Mono.empty()
    }
}

data class MarketData(val stock: String, val currentPrice: Int = 0) {

    companion object {
        fun fromException(e: Exception): MarketData {
            return MarketData(stock = e.message!!)
        }
    }
}

data class MarketDataRequest(val stock: String? = null)
