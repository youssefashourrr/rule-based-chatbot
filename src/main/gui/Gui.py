import streamlit as st
from py4j.java_gateway import JavaGateway, GatewayParameters

# Set page config must be the first Streamlit command
st.set_page_config(page_title="Scala Chat", page_icon="🤖", layout="centered")

@st.cache_resource
def get_scala_bot():
    gateway = JavaGateway(gateway_parameters=GatewayParameters(port=25333))
    return gateway.entry_point

bot = get_scala_bot()

st.title("🤖 Scala 3 Chatbot")

if "messages" not in st.session_state:
    st.session_state.messages = []

if not st.session_state.messages:
    greeting = bot.greetUser()
    st.session_state.messages.append({"role": "assistant", "content": greeting})

prompt = st.chat_input("Say something to the Scala bot...")

if prompt:
    st.session_state.messages.append({"role": "user", "content": prompt})
    try:
        response = bot.generateResponses(bot.handleUserInput((bot.parseInput(prompt))))
    except Exception as e:
        response = f"❌ Scala error: {e}"
    st.session_state.messages.append({"role": "assistant", "content": response})

for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])