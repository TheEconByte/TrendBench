import axios from "axios";

const API_BASE_URL = "http://localhost:8080";

export async function getHealth() {
    const response = await axios.get(`${API_BASE_URL}/api/health`);
    return response.data;
}