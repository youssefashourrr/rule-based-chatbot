import py4j.GatewayServer

@main def run(): Unit =
{
    val server = new GatewayServer(Chatbot, 25333)
    server.start()
}