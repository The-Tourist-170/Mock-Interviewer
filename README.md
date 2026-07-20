# AI Interviewer (Frontend)

This is the React frontend for the AI Interviewer application. It provides a modern, responsive interface for conducting AI-driven interviews.

**Live Demo:** [**https://ai-mern-interviewer.web.app/**](https://ai-mern-interviewer.web.app/)

> **Note:** The backend for the live demo is hosted on a free service. The server "sleeps" when inactive, so the first request you make might take up to 60 seconds while it wakes up.

---

## 🚀 Tech Stack

* **Frontend:** React, Vite, Redux, Tailwind CSS
* **Backend:** Java, Spring Boot, Maven

---

## 🛠️ How to Run Locally

Follow these steps to get the full application running on your machine.

### Prerequisites

* Java JDK 17+
* Apache Maven
* Node.js 18+

### 1. Run the Backend (Spring Boot)

First, get the backend server running.

1.  **Clone the backend repository** (replace with your actual repo URL):
    ```sh
    git clone [https://github.com/your-username/ai-interviewer-backend.git](https://github.com/your-username/ai-interviewer-backend.git)
    cd ai-interviewer-backend
    ```

2.  **Configure your API Key.** The AI features require an API key. Add it to the `application.properties` file located at `src/main/resources/`.
    ```properties
    # src/main/resources/application.properties
    openai.api.key=sk-YourSecretApiKeyHere
    server.port=8080
    ```

3.  **Run the server** using Maven:
    ```sh
    mvn spring-boot:run
    ```
    The backend should now be running on `http://localhost:8080`.

### 2. Run the Frontend (React)

With the backend running, you can now start the frontend.

1.  **Clone this frontend repository:**
    ```sh
    git clone [https://github.com/your-username/ai-interviewer-frontend.git](https://github.com/your-username/ai-interviewer-frontend.git)
    cd ai-interviewer-frontend
    ```

2.  **Install dependencies:**
    ```sh
    npm install
    ```

3.  **Link to your backend.** Create a file named `.env.local` in the root of the frontend project and add the following line:
    ```
    VITE_API_BASE_URL=http://localhost:8080
    ```

4.  **Start the app:**
    ```sh
    npm run dev
    ```
    The frontend will be available at `http://localhost:5173`.
