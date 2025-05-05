import streamlit as st
import random
from py4j.java_gateway import JavaGateway, GatewayParameters
from py4j.java_collections import JavaList
from py4j.java_collections import ListConverter



# Set page config
st.set_page_config(
    page_title="SportsBot ⚽",
    page_icon="⚽",
    layout="centered"
)

@st.cache_resource
def get_scala_bot():
    gateway = JavaGateway(gateway_parameters=GatewayParameters(port=25333))
    return gateway,gateway.entry_point

gateway, bot = get_scala_bot()

# Title section
st.title("⚽ SportsBot 3000")
st.markdown("*Your ultimate ChatBot sports companion!*")

# Initialize chat
if "messages" not in st.session_state:
    st.session_state.messages = []
    st.session_state.messages.append({"role": "assistant", "content": bot.greetUser()})

for i, msg in enumerate(st.session_state.messages):
    # Show message
    avatar = "⚽" if msg["role"] == "assistant" else "👤"
    with st.chat_message(msg["role"], avatar=avatar):
        st.write(msg["content"])

        # Quiz section under assistant messages
        if msg["role"] == "assistant":
            with st.expander("⚡ Quick Quiz", expanded=False):
                # Sport selection (3 columns)
                st.write("Choose a sport:")
                sport_cols = st.columns(3)
                sports = [
                    ("Soccer", "⚽", "soccer"),
                    ("Basketball", "🏀", "basketball"),
                    ("Tennis", "🎾", "tennis")
                ]
                mode = random.choice([True, False])

                for col, (sport, emoji, sport_key) in zip(sport_cols, sports):
                    with col:
                        if st.button(
                                f"{emoji} {sport}",
                                key=f"sport_{sport_key}_{i}",
                                use_container_width=True,
                                help=f"Take a {sport} quiz"
                        ):
                            quiz_data = bot.getQuiz().selectQuizQuestions(sport_key, mode)
                            question = quiz_data._1()
                            options = list(quiz_data._2())

                            st.session_state.current_quiz = {
                                "question": question,
                                "options": options,
                                "sport": sport_key
                            }
                            st.rerun()

                # Display current quiz if exists
                if "current_quiz" in st.session_state:
                    st.markdown(f"**Question:** {st.session_state.current_quiz['question']}")
                    options = st.session_state.current_quiz['options']

                    if options:
                        for row in range(0, len(options), 2):
                            cols = st.columns(2)
                            row_options = options[row:row+2]

                            for col, opt in zip(cols, row_options):
                                with col:
                                    if st.button(
                                            opt,
                                            key=f"ans_{st.session_state.current_quiz['sport']}_{i}_{row}_{opt}",
                                            use_container_width=True
                                    ):
                                        is_correct = bot.getQuiz().evaluateQuizAnswer(opt)
                                        feedback = "✅ Correct!" if is_correct else "❌ Incorrect!"
                                        st.session_state.messages.append({"role": "assistant", "content": feedback})
                                        del st.session_state.current_quiz
                                        st.rerun()
                    else:
                        user_answer = st.text_input(
                            "Your Answer:",
                            key=f"text_input_{st.session_state.current_quiz['sport']}_{i}"
                        )
                        if st.button("Submit Answer", key=f"submit_text_{i}"):
                            if user_answer.strip():
                                is_correct = bot.getQuiz().evaluateQuizAnswer(bot.parseInput(user_answer.strip()))
                                feedback = "✅ Correct!" if is_correct else "❌ Incorrect!"
                                st.session_state.messages.append({"role": "assistant", "content": feedback})
                                del st.session_state.current_quiz
                                st.rerun()
                            else:
                                st.warning("Please enter an answer before submitting.")

# Chat input
if prompt := st.chat_input("Ask about sports..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    try:
        response = bot.generateResponse(bot.handleUserInput((bot.parseInput(prompt))))
        st.session_state.messages.append({"role": "assistant", "content": response})
    except Exception as e:
        st.session_state.messages.append({"role": "assistant", "content": f"Error: {e}"})
    st.rerun()