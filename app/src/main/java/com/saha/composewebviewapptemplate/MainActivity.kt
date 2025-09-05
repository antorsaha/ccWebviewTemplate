package com.saha.composewebviewapptemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.saha.composewebviewapptemplate.screens.WebViewScreen
import com.saha.composewebviewapptemplate.screens.WebViewContent
import com.saha.composewebviewapptemplate.screens.ContentType
import com.saha.composewebviewapptemplate.ui.theme.ComposeWebViewAppTemplateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeWebViewAppTemplateTheme {
                // Example 1: Load from URL
                WebViewScreen("https://dubai-artists.i-mbu.online")
                
                // Example 2: Load HTML content
                //WebViewScreen(htmlContent = getSampleHTML())
                
                // Example 3: Load from assets
                // WebViewScreen(WebViewContent(ContentType.ASSET, "sample.html"))
            }
        }
    }
    
    // Sample HTML content
    private fun getSampleHTML(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Sample HTML</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 20px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                }
                .container {
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background: rgba(255, 255, 255, 0.1);
                    border-radius: 10px;
                    backdrop-filter: blur(10px);
                }
                h1 {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .feature {
                    background: rgba(255, 255, 255, 0.2);
                    padding: 15px;
                    margin: 10px 0;
                    border-radius: 8px;
                }
                button {
                    background: #4CAF50;
                    color: white;
                    padding: 10px 20px;
                    border: none;
                    border-radius: 5px;
                    cursor: pointer;
                    margin: 5px;
                }
                button:hover {
                    background: #45a049;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1> WebView Template</h1>
                <p>This is a sample HTML content loaded in your WebView!</p>
                
                <div class="feature">
                    <h3>✨ Features</h3>
                    <ul>
                        <li>Load URLs</li>
                        <li>Load HTML content</li>
                        <li>Load from assets</li>
                        <li>Load from files</li>
                        <li>JavaScript support</li>
                        <li>File upload support</li>
                        <li>Camera integration</li>
                    </ul>
                </div>
                
                <div class="feature">
                    <h3> Interactive Demo</h3>
                    <button onclick="showAlert()">Show Alert</button>
                    <button onclick="changeColor()">Change Color</button>
                    <button onclick="loadExternal()">Load External Site</button>
                </div>
                
                <div id="demo" class="feature">
                    <h3>📱 Demo Area</h3>
                    <p id="demo-text">Click the buttons above to see JavaScript in action!</p>
                </div>
            </div>
            
            <script>
                function showAlert() {
                    alert('Hello from WebView! JavaScript is working perfectly!');
                }
                
                function changeColor() {
                    const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57'];
                    const randomColor = colors[Math.floor(Math.random() * colors.length)];
                    document.getElementById('demo').style.background = randomColor;
                    document.getElementById('demo-text').innerHTML = 'Color changed to: ' + randomColor;
                }
                
                function loadExternal() {
                    if (confirm('This will load an external website. Continue?')) {
                        window.location.href = 'https://www.google.com';
                    }
                }
                
                // Add some dynamic content
                document.addEventListener('DOMContentLoaded', function() {
                    const time = new Date().toLocaleString();
                    document.getElementById('demo-text').innerHTML += '<br><small>Page loaded at: ' + time + '</small>';
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeWebViewAppTemplateTheme {
        Greeting("Android")
    }
}