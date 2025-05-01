import streamlit as st
from py4j.java_gateway import JavaGateway, GatewayParameters

# Set page config
st.set_page_config(
    page_title="SportsBot ⚽",
    page_icon="⚽",
    layout="centered"
)

@st.cache_resource
def get_scala_bot():
    gateway = JavaGateway(gateway_parameters=GatewayParameters(port=25333))
    return gateway.entry_point

bot = get_scala_bot()

# Title section
st.title("⚽ SportsBot 3000")
st.markdown("*Your ultimate ChatBot sports companion!*")

# Initialize chat
if "messages" not in st.session_state:
    st.session_state.messages = []
    greeting = bot.greetUser()
    st.session_state.messages.append({"role": "assistant", "content": greeting})

# Display messages with expandable quiz buttons
for i, msg in enumerate(st.session_state.messages):
    # Show message
    avatar = "⚽" if msg["role"] == "assistant" else "👤"
    with st.chat_message(msg["role"], avatar=avatar):
        st.write(msg["content"])

        # Add expandable quiz section under assistant messages
        if msg["role"] == "assistant":
            with st.expander("⚡ Quick Quiz", expanded=False):
                col1, col2, col3 = st.columns(3)
                with col1:
                    if st.button("Football Rules ⚽", key=f"football_{i}"):
                        st.session_state.messages.append({"role": "user", "content": "Explain the basic rules of football"})
                        st.rerun()
                with col2:
                    if st.button("NBA Champions 🏀", key=f"basketball_{i}"):
                        st.session_state.messages.append({"role": "user", "content": "Who won the last NBA championship?"})
                        st.rerun()
                with col3:
                    if st.button("Tennis Stars 🎾", key=f"tennis_{i}"):
                        st.session_state.messages.append({"role": "user", "content": "Who are the current top tennis players?"})
                        st.rerun()

# Chat input
if prompt := st.chat_input("Ask about sports..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    try:
        response = bot.generateResponse(bot.handleUserInput((bot.parseInput(prompt))))
        st.session_state.messages.append({"role": "assistant", "content": response})
    except Exception as e:
        st.session_state.messages.append({"role": "assistant", "content": f"Error: {e}"})
    st.rerun()