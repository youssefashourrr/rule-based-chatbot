import streamlit as st
import random
from py4j.java_gateway import JavaGateway, GatewayParameters
import matplotlib.pyplot as plt
import pandas as pd


# Page setup
st.set_page_config(
    page_title="SportsBot ⚽",
    page_icon="⚽",
    layout="wide",
    initial_sidebar_state="expanded"
)

@st.cache_resource
def get_scala_bot():
    gateway = JavaGateway(gateway_parameters=GatewayParameters(port=25333, auto_convert=True))
    return gateway, gateway.entry_point

try:
    gateway, bot = get_scala_bot()
except Exception as e:
    st.error(f"Could not connect to Scala backend: {e}. Please ensure the Scala application is running and accessible on port 25333.")
    st.stop()

# Style
st.markdown("""
    <style>
        .main > div {
            padding-bottom: 120px;
        }
        .chat-container {
            height: calc(100vh - 280px);
            overflow-y: auto;
            padding: 15px;
            margin-bottom: 20px;
            border: 1px solid #333;
            border-radius: 5px;
        }
        .quiz-container {
            height: calc(100vh - 230px);
            overflow-y: auto;
            padding: 20px;
            border: 1px solid #333;
            border-radius: 5px;
        }
        .fixed-input {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            width: auto;
            background: #0e1117;
            padding: 10px 20px;
            border-top: 1px solid #333;
            z-index: 1000;
        }
        @media (min-width: 576px) {
            .main .fixed-input {
                left: 305px;
            }
        }
    </style>
""", unsafe_allow_html=True)

# Session state initialization
if "messages" not in st.session_state:
    try:
        st.session_state.messages = [{"role": "assistant", "content": bot.greetUser()}]
    except Exception as e:
        st.session_state.messages = [{"role": "assistant", "content": f"Error greeting user via Scala backend: {e}"}]

if "current_view" not in st.session_state:
    st.session_state.current_view = "chat"
if "current_quiz" not in st.session_state:
    st.session_state.current_quiz = None
if "quiz_feedback" not in st.session_state:
    st.session_state.quiz_feedback = None
if "quiz_summary" not in st.session_state:
    st.session_state.quiz_summary = None

# Sidebar navigation
with st.sidebar:
    st.title("⚽ SportsBot")
    st.markdown("---")
    if st.button("💬 Chat Window", key="nav_chat_btn", use_container_width=True,
                type="primary" if st.session_state.current_view == "chat" else "secondary"):
        st.session_state.current_view = "chat"
        st.rerun()
    if st.button("❓ Quiz Window", key="nav_quiz_btn", use_container_width=True,
                type="primary" if st.session_state.current_view == "quiz" else "secondary"):
        st.session_state.current_view = "quiz"
        st.rerun()
    if st.button("📊 Quiz Analysis", key="nav_analysis_btn", use_container_width=True,
                type="primary" if st.session_state.current_view == "analysis" else "secondary"):
        st.session_state.current_view = "analysis"
        st.rerun()
    st.markdown("---")
    with st.expander("ℹ️ About"):
        st.write("**Hamoodyyyy**")

# Chat Window
if st.session_state.current_view == "chat":
    st.title("💬 Sports Chat")
    st.caption("Ask me anything about sports!")

    chat_display_container = st.container()
    with chat_display_container:
        for msg in st.session_state.messages:
            avatar = "⚽" if msg["role"] == "assistant" else "👤"
            with st.chat_message(msg["role"], avatar=avatar):
                st.write(msg["content"])

    prompt = st.chat_input("Ask about sports...", key="chat_input_main_window")
    if prompt:
        st.session_state.messages.append({"role": "user", "content": prompt})
        st.rerun()

    if st.session_state.messages and st.session_state.messages[-1]["role"] == "user":
        last_user_prompt = st.session_state.messages[-1]["content"]
        try:
            # Scala backend handles input processing
            response = bot.generateResponse(last_user_prompt)
            st.session_state.messages.append({"role": "assistant", "content": response})
        except Exception as e:
            st.session_state.messages.append({"role": "assistant", "content": f"Error communicating with bot: {e}"})
        st.rerun()

# Quiz Window
elif st.session_state.current_view == "quiz":
    st.title("❓ Sports Quiz")
    st.caption("Test your sports knowledge!")

    quiz_area = st.container()
    with quiz_area:
        if st.session_state.current_quiz:
            quiz_data = st.session_state.current_quiz
            sport_context = quiz_data.get("sport", "general")
            question_text_for_key = quiz_data.get("question", "default_question_key")
            question_key_part = str(hash(question_text_for_key))[-8:]

            if quiz_data.get("answered"):
                st.markdown("---")
                st.subheader(f"Result for {quiz_data.get('sport', 'Sport').capitalize()} Quiz")
                if st.session_state.quiz_feedback:
                    if "Correct" in st.session_state.quiz_feedback:
                        st.success(st.session_state.quiz_feedback)
                    else:
                        st.error(st.session_state.quiz_feedback)
                else:
                    st.info("Answer processed. No detailed feedback available.")

                st.info("Quiz question completed.")
                if st.button("Try another quiz / Select Sport", key=f"next_quiz_btn_{sport_context}"):
                    st.session_state.current_quiz = None
                    st.session_state.quiz_feedback = None
                    st.rerun()
            else:
                st.markdown("---")
                st.subheader(f"Question for {quiz_data.get('sport', 'Sport').capitalize()}")
                st.markdown(f"### {quiz_data.get('question', 'No question text available.')}")

                options = quiz_data.get("options", [])

                if options and isinstance(options, list) and len(options) > 0:
                    st.write("Choose an answer or type manually:")

                    for i, opt_text_raw in enumerate(options):
                        opt_text = str(opt_text_raw)
                        button_key = f"ans_opt_{i}_{sport_context}_{question_key_part}"
                        if st.button(opt_text, key=button_key, use_container_width=True):
                            try:
                                # Evaluate the quiz answer via the Scala backend
                                is_correct = bot.getQuiz().evaluateQuizAnswer(opt_text.strip())
                                feedback = "✅ Correct! Well done!" if is_correct else "❌ Incorrect. Better luck next time!"
                                st.session_state.quiz_feedback = feedback
                                st.session_state.current_quiz["answered"] = True
                                st.rerun()
                            except Exception as e:
                                st.error(f"Error evaluating answer: {e}")
                                st.session_state.quiz_feedback = f"Error during evaluation: {e}"
                                st.session_state.current_quiz["answered"] = True
                                st.rerun()

                    form_key = f"quiz_mcq_manual_form_{sport_context}_{question_key_part}"
                    with st.form(key=form_key):
                        user_answer = st.text_input("Or type your answer here:")
                        submit_button = st.form_submit_button("Submit Manual Answer")

                        if submit_button:
                            if user_answer.strip():
                                normalized_input = user_answer.strip().lower()
                                normalized_options = [opt.strip().lower() for opt in options]

                                matched_option = None
                                for original_option in options:
                                    if normalized_input == original_option.strip().lower():
                                        matched_option = original_option
                                        break

                                if matched_option:
                                    try:
                                        # Evaluate the manual answer via the Scala backend
                                        is_correct = bot.getQuiz().evaluateQuizAnswer(matched_option)
                                        feedback = "✅ Correct! Well done!" if is_correct else "❌ Incorrect. Better luck next time!"
                                        st.session_state.quiz_feedback = feedback
                                        st.session_state.current_quiz["answered"] = True
                                        st.rerun()
                                    except Exception as e:
                                        st.error(f"Error evaluating manual answer: {e}")
                                        st.session_state.quiz_feedback = f"Error during evaluation: {e}"
                                        st.session_state.current_quiz["answered"] = True
                                        st.rerun()
                                else:
                                    st.warning("⚠️ Your answer doesn't exactly match any of the provided choices. Please check your spelling and try again.")
                            else:
                                st.warning("Please enter an answer before submitting.")
                else:
                    st.write("Type your answer below:")
                    form_key = f"quiz_fr_form_{sport_context}_{question_key_part}"
                    with st.form(key=form_key):
                        user_answer = st.text_input("Your Answer:")
                        submit_button = st.form_submit_button("Submit Answer")

                        if submit_button:
                            if user_answer.strip():
                                try:
                                    # Evaluate the free response answer via Scala backend
                                    is_correct = bot.getQuiz().evaluateQuizAnswer(user_answer.strip())
                                    feedback = "✅ Correct! Well done!" if is_correct else "❌ Incorrect. Better luck next time!"
                                    st.session_state.quiz_feedback = feedback
                                    st.session_state.current_quiz["answered"] = True
                                    st.rerun()
                                except Exception as e:
                                    st.error(f"Error evaluating answer: {e}")
                                    st.session_state.quiz_feedback = f"Error during evaluation: {e}"
                                    st.session_state.current_quiz["answered"] = True
                                    st.rerun()
                            else:
                                st.warning("Please enter an answer before submitting.")
        else:
            st.header("⚡ Quick Quiz")
            st.write("Choose a sport to start a quiz:")

            sports = [
                ("Soccer", "⚽", "soccer"),
                ("Basketball", "🏀", "basketball"),
                ("Tennis", "🎾", "tennis")
            ]
            cols = st.columns(len(sports))
            for i, (sport_name, emoji, sport_key) in enumerate(sports):
                with cols[i]:
                    button_key = f"sport_select_btn_{sport_key}"
                    if st.button(f"{emoji} {sport_name}", key=button_key, use_container_width=True):
                        try:
                            fetch_mcq_type = random.choice([True, False])
                            raw_quiz_data = bot.getQuiz().selectQuizQuestions(sport_key, fetch_mcq_type)
                            question_text = raw_quiz_data._1()
                            raw_options = raw_quiz_data._2()

                            if not isinstance(question_text, str) or not question_text.strip():
                                st.error(f"Invalid or empty question text from backend for {sport_name}.")
                                continue

                            options_list = []
                            if raw_options is not None:
                                try:
                                    converted_options = list(raw_options)
                                    if isinstance(converted_options, list) and len(converted_options) > 0:
                                        options_list = [str(opt).strip() for opt in converted_options if str(opt).strip()]
                                        if not options_list:
                                            st.warning(f"MCQ options for {sport_name} were empty. Treating as free-response.")
                                except Exception as conversion_error:
                                    st.warning(f"Could not convert options to list for {sport_name} (Error: {conversion_error}). Assuming free-response.")

                            st.session_state.current_quiz = {
                                "question": question_text,
                                "options": options_list,
                                "sport": sport_key,
                                "answered": False
                            }
                            st.session_state.quiz_feedback = None
                            st.rerun()
                        except Exception as e:
                            st.error(f"Error starting {sport_name} quiz: {e}")
                            st.session_state.current_quiz = None

# 📊 Quiz Analysis Window
elif st.session_state.current_view == "analysis":
    import re

    st.title("📊 Quiz Performance Summary")
    st.caption("See how you've been doing in your quizzes.")

    try:
        summary_data = bot.getQuiz().analyzeQuizPerformance()

        if summary_data is not None:
            summary_list = list(summary_data)

            if summary_list and hasattr(summary_list[0], "_1") and hasattr(summary_list[0], "_2"):
                # Separate metrics from Scala tuples
                metrics = [(item._1(), item._2()) for item in summary_list]

                # Parse the success rate (first element)
                success_rate = None
                category_data = []

                for metric in metrics:
                    if isinstance(metric[1], (int, float)) and metric[0].lower().startswith("success"):
                        success_rate = float(metric[1])
                    else:
                        category_data.append((metric[0], float(metric[1])))

                # Set default values
                correct = round(success_rate) if success_rate is not None else 0
                incorrect = 100 - correct

                st.subheader("📋 Summary Stats")
                st.write(f"✅ **Correct Answers**: {correct} %")
                st.write(f"❌ **Incorrect Answers**: {incorrect} %")

                # Pie chart - correct vs incorrect
                if correct + incorrect > 0:
                    col1, col2 = st.columns([1, 2])
                    with col1:
                        st.subheader("Correct vs Incorrect")
                        fig1, ax1 = plt.subplots(figsize=(2, 2))
                        ax1.pie(
                            [correct, incorrect],
                            labels=['Correct', 'Incorrect'],
                            autopct='%1.1f%%',
                            startangle=90,
                            colors=['#4CAF50', '#F44336'],
                            textprops=dict(color="white", fontsize=8)
                        )
                        ax1.axis('equal')
                        st.pyplot(fig1, use_container_width=True)

                    # Pie chart - top 3 categories
                    if category_data:
                        with col2:
                            st.subheader("Top 3 Asked Categories")
                            labels = [cat for cat, _ in category_data]
                            values = [pct for _, pct in category_data]

                            fig2, ax2 = plt.subplots(figsize=(3, 3))
                            ax2.pie(
                                values,
                                labels=labels,
                                autopct='%1.1f%%',
                                startangle=140,
                                colors=plt.cm.Set3.colors[:len(labels)],
                                textprops=dict(color="black", fontsize=8)
                            )
                            ax2.axis('equal')
                            st.pyplot(fig2, use_container_width=True)

            else:
                st.warning("Unknown summary format returned from backend.")
        else:
            st.info("No quiz summary available.")

    except Exception as e:
        st.error(f"Error processing quiz performance: {e}")
