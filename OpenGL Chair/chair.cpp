/**
 * CS 330 Final Project
 *
 * @author David McWilliams
 */
#include "SOIL2/SOIL2.h"
#include <iostream>
#include <GL/glew.h>
#include <GL/freeglut.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

// Wood texture from CC0 Textures
// https://cc0textures.com/view?id=Wood025
const char* TEXTURE_FILE = "wood.jpg";

// Constants
const char* WINDOW_TITLE = "David McWilliams - CS 330 Final Project";
const int WINDOW_WIDTH = 800;
const int WINDOW_HEIGHT = 600;
const GLfloat MIN_ZOOM = 2.0f;
const GLfloat MAX_ZOOM = 50.0f;
const GLfloat ZOOM_SENSITIVITY = 0.1f;
const GLfloat ROTATE_SENSITIVITY = 0.005f;
const glm::vec3 CAMERA_CENTER = glm::vec3(0.0f, 0.0f, 0.0f);
const glm::vec3 CAMERA_UP_Y = glm::vec3(0.0f, 1.0f, 0.0f);

// Rectangles perpendicular to the X-axis
GLfloat rectX[] = {
  // X      Corner 1    Corner 2
    45,     -5, -55,     5,  35,  // Seat side
    55,    -80, -55,   170, -45,  // Back pole outside
    45,    -80, -45,   170, -55,  // Back pole inside
    55,    -80,  25,    40,  35,  // Front pole outside
    45,    -80,  35,    40,  25,  // Front pole inside
    60,     40, -45,    45,  45,  // Armrest outside
    40,     40,  45,    45, -45,  // Armrest inside
    53,    -40, -45,   -34,  25,  // Side crossbar outside
    47,    -40,  25,   -34, -45,  // Side crossbar inside
    15,     40, -51,   130, -49,  // Backrest bar 1 outside
     5,     40, -49,   130, -51,  // Backrest bar 1 inside
    35,     40, -51,   130, -49,  // Backrest bar 2 outside
    25,     40, -49,   130, -51,  // Backrest bar 2 inside
};

// Rectangles perpendicular to the Y-axis
GLfloat rectY[] = {
  // Y      Corner 1    Corner 2
     5,      0, -55,    45,  35,  // Seat top
    -5,      0,  35,    45, -55,  // Seat bottom
    45,     40, -45,    60,  45,  // Armrest top
    40,     40,  45,    60, -45,  // Armrest bottom
    40,      0, -53,    45, -47,  // Backrest lower bar top
    25,      0, -47,    45, -53,  // Backrest lower bar bottom
   160,      0, -53,    45, -47,  // Backrest upper bar top
   130,      0, -47,    45, -53,  // Backrest upper bar bottom
   -80,     45,  35,    55,  25,  // Front pole bottom
   170,     45, -55,    55, -45,  // Back pole top
   -80,     45, -45,    55, -55,  // Back pole bottom
   -34,     47, -45,    53,  25,  // Side crossbar top
   -40,     47,  25,    53, -45,  // Side crossbar bottom
   -40,      0,  27,    45,  33,  // Front crossbar top
   -46,      0,  33,    45,  27,  // Front crossbar bottom
   -40,      0, -53,    45, -47,  // Back crossbar top
   -46,      0, -47,    45, -53,  // Back crossbar bottom
};

// Rectangles perpendicular to the Z-axis
GLfloat rectZ[] = {
  // Z      Corner 1    Corner 2
    35,      0,  -5,    45,   5,  // Seat front
   -55,      0,   5,    45,  -5,  // Seat back
    45,     40,  40,    60,  45,  // Armrest front
   -45,     40,  45,    60,  40,  // Armrest back
   -47,      0,  25,    45,  40,  // Backrest lower bar front
   -53,      0,  40,    45,  25,  // Backrest lower bar back
   -47,      0, 130,    45, 160,  // Backrest upper bar front
   -53,      0, 160,    45, 130,  // Backrest upper bar back
   -45,     45, -80,    55, 170,  // Back pole front
   -55,     45, 170,    55, -80,  // Back pole back
    35,     45, -80,    55,  40,  // Front pole front
    25,     45,  40,    55, -80,  // Front pole back
   -49,      5,  40,    15, 130,  // Backrest bar 1 front
   -51,      5, 130,    15,  40,  // Backrest bar 1 back
   -49,     25,  40,    35, 130,  // Backrest bar 2 front
   -51,     25, 130,    35,  40,  // Backrest bar 2 back
    33,      0, -46,    45, -40,  // Front crossbar front
    27,      0, -40,    45, -46,  // Front crossbar back
   -47,      0, -46,    45, -40,  // Back crossbar front
   -53,      0, -40,    45, -46,  // Back crossbar back
};

// Object declarations
GLint objectShaderProgram;
GLuint VAO;
GLuint VBO;
GLuint texture;

// Global variables
GLfloat aspectRatio = WINDOW_WIDTH / WINDOW_HEIGHT;
bool orthoProjection = false;
bool rotateButtonPressed = false;
bool zoomButtonPressed = false;
GLint rotationStartX;
GLint rotationStartY;
GLfloat totalYaw = 0.0f;
GLfloat totalPitch = 0.0f;
GLfloat totalZoom = 0.0f;
GLfloat zoomScale = 10.0f;
glm::vec3 cameraDirection = glm::vec3(0.0f, 0.0f, 1.0f);
int numVertices;

// Function declarations
void ResizeWindow(int, int);
void RenderGraphics();
void CreateShaders();
void CreateTextures();
void CreateBuffers();
void DestroyBuffers();
void MouseClick(int button, int state, int x, int y);
void MousePressedMove(int x, int y);
void Keyboard(unsigned char key, int x, int y);

// Shader macro
#ifndef GLSL
#define GLSL(Version, Source) "#version " #Version "\n" #Source
#endif

// Vertex shader definition
const GLchar *VertexShader = GLSL(440,
    // Position and color input
    in layout(location=0) vec3 position;
    in layout(location=1) vec2 textureInput;
    in layout(location=2) vec3 normalInput;

    // Color output
    out vec3 positionFromVShader;
    out vec2 textureFromVShader;
    out vec3 normalFromVShader;

    // Matrices for transforming vertex data
    uniform mat4 projection;
    uniform mat4 view;
    uniform mat4 model;

    void main() {
        // Write position to GPU
        gl_Position = projection * view * model * vec4(position, 1.0f);

        // Pass only model position to fragment shader (exclude view and projection)
        positionFromVShader = vec3(model * vec4(position, 1.0f));

        // Flip texture and pass it to fragment shader
        textureFromVShader = vec2(textureInput.x, 1.0f - textureInput.y);

        // Pass normals to fragment shader
        normalFromVShader = mat3(transpose(inverse(model))) * normalInput;
    }
);

// Object fragment shader definition
const GLchar *FragmentShader = GLSL(440,
    // Vertex color input
    in vec3 positionFromVShader;
    in vec2 textureFromVShader;
    in vec3 normalFromVShader;

    // Color output
    out vec4 gpuColor;

    uniform mat4 view;
    uniform sampler2D uTexture;

    void phong(in vec3 position, in vec3 color, in float ambientStrength, in float intensity, out vec3 phong) {
        // Calculate ambient lighting
        vec3 ambient = ambientStrength * color;

        // Calculate diffuse lighting
        vec3 norm = normalize(normalFromVShader);
        vec3 lightDirection = normalize(position - positionFromVShader);
        float impact = max(dot(norm, lightDirection), 0.0f);
        vec3 diffuse = impact * color;

        // Calculate specular lighting
        float specularIntensity = 0.5f;
        float highlightSize = 16.0f;
        vec3 viewDir = normalize(vec3(view[0][0], view[0][1], view[0][2]) - positionFromVShader);
        vec3 reflectDir = reflect(-lightDirection, norm);
        float specularComponent = pow(max(dot(viewDir, reflectDir), 0.0f), highlightSize);
        vec3 specular = specularIntensity * specularComponent * color;

        // Calculate phong result
        phong = (ambient + diffuse + specular) * intensity;
    }

    void main() {
        // White Lamp
        vec3 whiteLampPos = vec3(10.0f, 10.0f, 5.0f);
        vec3 whiteLampColor = vec3(1.0f, 1.0f, 1.0f);
        vec3 whiteLampPhong;

        // Green lamp
        vec3 greenLampPos = vec3(-10.0f, 10.0f, -5.0f);
        vec3 greenLampColor = vec3(0.0f, 1.0f, 0.0f);
        vec3 greenLampPhong;

        // Calculate phong result
        phong(whiteLampPos, whiteLampColor, 0.6f, 1.0f, whiteLampPhong);
        phong(greenLampPos, greenLampColor, 0.0f, 0.5f, greenLampPhong);

        // Send final color to GPU
        gpuColor = vec4(whiteLampPhong + greenLampPhong, 1.0f) * texture(uTexture, textureFromVShader);
    }
);

/**
 * Main function
 */
int main(int argc, char *argv[]) {
    // Set up window
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_DEPTH | GLUT_DOUBLE | GLUT_RGBA);
    glutInitWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    glutCreateWindow(WINDOW_TITLE);

    // Set up glew
    glewExperimental = GL_TRUE;
    GLenum glewInitResult = glewInit();
    if (glewInitResult != GLEW_OK) {
        fprintf(stderr, "ERROR: %s\n", glewGetErrorString(glewInitResult));
        return -1;
    }

    // Set up callback functions
    glutReshapeFunc(ResizeWindow);
    glutDisplayFunc(RenderGraphics);
    glutKeyboardFunc(Keyboard);
    glutMouseFunc(MouseClick);
    glutMotionFunc(MousePressedMove);

    // Ignore held down key
    glutSetKeyRepeat(false);

    // Set background color to black
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    // Hide background fragments
    glEnable(GL_DEPTH_TEST);

    CreateShaders();
    CreateTextures();
    CreateBuffers();

    // Enter main loop
    glutMainLoop();

    DestroyBuffers();

    return 0;
}

/**
 * Handle window resize
 */
void ResizeWindow(int width, int height) {
    aspectRatio = GLfloat(width) / height;
    glViewport(0, 0, width, height);
}

/*
 * Calculate yaw angle
 *
 * @param x The mouse x position in pixels
 * @return The yaw angle in radians
 */
float getYaw(int x) {
    float offsetX = (rotationStartX - x) * ROTATE_SENSITIVITY;
    return totalYaw + offsetX;
}

/*
 * Calculate pitch angle
 * Limited to an angle of pi/2 radians (90 degrees) or less
 *
 * @param y The mouse y position in pixels
 * @return The pitch angle in radians
 */
float getPitch(int y) {
    float offsetY = (y - rotationStartY) * ROTATE_SENSITIVITY;
    float pitch = totalPitch + offsetY;

    // Clamp to between -pi/2 and pi/2 radians
    if (pitch < -glm::pi<float>() / 2) {
        pitch = -glm::pi<float>() / 2;
    }
    if (pitch > glm::pi<float>() / 2) {
        pitch = glm::pi<float>() / 2;
    }

    return pitch;
}

/*
 * Handle mouse click
 */
void MouseClick(int button, int state, int x, int y) {
    switch (button) {
    case GLUT_LEFT_BUTTON:
        // Rotation controls
        if (state == GLUT_DOWN) {
            // Start tracking rotation
            rotateButtonPressed = true;
            rotationStartX = x;
            rotationStartY = y;
        } else {
            // Stop tracking rotation and save current value
            rotateButtonPressed = false;
            totalYaw = getYaw(x);
            totalPitch = getPitch(y);
        }
        glutPostRedisplay();
        break;

    case 3: // Mouse wheel up
        if (state == GLUT_DOWN) {
            // Zoom in
            zoomScale /= 1 + ZOOM_SENSITIVITY;
            if (zoomScale < MIN_ZOOM) {
                zoomScale = MIN_ZOOM;
            }
            glutPostRedisplay();
        }
        break;

    case 4: // Mouse wheel down
        if (state == GLUT_DOWN) {
            // Zoom out
            zoomScale *= 1 + ZOOM_SENSITIVITY;
            if (zoomScale >= MAX_ZOOM) {
                zoomScale = MAX_ZOOM;
            }
            glutPostRedisplay();
        }
        break;
    }
}

/*
 * Handle mouse drag
 */
void MousePressedMove(int x, int y) {
    // Rotation controls
    if (rotateButtonPressed) {
        float yaw = getYaw(x);
        float pitch = getPitch(y);
        cameraDirection.x = sin(yaw);
        cameraDirection.y = sin(pitch);
        cameraDirection.z = cos(yaw);
        glutPostRedisplay();
    }
}

/**
 * Handle keyboard input
 */
void Keyboard(unsigned char key, int x, int y) {
    // Orthographic projection
    if (key == 'o') {
        orthoProjection = true;
        glutPostRedisplay();
    }
    // Perspective projection
    if (key == 'p') {
        orthoProjection = false;
        glutPostRedisplay();
    }
}

/**
 * Handle graphics rendering
 */
void RenderGraphics() {
    // Clear the screen
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Activate object vertex array
    glBindVertexArray(VAO);

    // Begin object shader program
    glUseProgram(objectShaderProgram);

    // Transform the object
    glm::mat4 model = glm::mat4(1.0f);
    model = glm::translate(model, glm::vec3(0.0f, -1.0f, 0.0f));
    model = glm::rotate(model, 0.0f, glm::vec3(0.0f, 1.0f, 0.0f));
    model = glm::scale(model, glm::vec3(0.03f, 0.03f, 0.03f));
    GLuint modelLocation = glGetUniformLocation(objectShaderProgram, "model");
    glUniformMatrix4fv(modelLocation, 1, GL_FALSE, glm::value_ptr(model));

    // Transform the camera
    glm::mat4 view = glm::lookAt(cameraDirection * zoomScale, CAMERA_CENTER, CAMERA_UP_Y);
    GLuint viewLocation = glGetUniformLocation(objectShaderProgram, "view");
    glUniformMatrix4fv(viewLocation, 1, GL_FALSE, glm::value_ptr(view));

    // Create projection
    glm::mat4 projection;
    if (orthoProjection) {
        GLfloat right = zoomScale * aspectRatio / 2;
        GLfloat top = zoomScale / 2;
        projection = glm::ortho(-right, right, -top, top, 0.1f, 100.0f);
    } else {
        projection = glm::perspective(45.0f, aspectRatio, 0.1f, 100.0f);
    }
    GLuint projectionLocation = glGetUniformLocation(objectShaderProgram, "projection");
    glUniformMatrix4fv(projectionLocation, 1, GL_FALSE, glm::value_ptr(projection));

    // Draw texture
    glBindTexture(GL_TEXTURE_2D, texture);

    // Draw triangles
    glDrawArrays(GL_TRIANGLES, 0, numVertices);

    // Draw another copy mirrored in the negative X direction
    model[0] *= -1;
    glUniformMatrix4fv(modelLocation, 1, GL_FALSE, glm::value_ptr(model));
    glDrawArrays(GL_TRIANGLES, 0, numVertices);

    // Deactivate vertex array
    glBindVertexArray(0);

    // Move the working buffer to the display buffer
    glutSwapBuffers();
}

/**
 * Create shaders
 */
void CreateShaders() {
    // Create shader program objects
    objectShaderProgram = glCreateProgram();

    // Create object vertex shader
    GLuint objectVertexShaderId = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(objectVertexShaderId, 1, &VertexShader, NULL);
    glCompileShader(objectVertexShaderId);
    glAttachShader(objectShaderProgram, objectVertexShaderId);

    // Create object fragment shader
    GLuint objectFragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(objectFragmentShaderId, 1, &FragmentShader, NULL);
    glCompileShader(objectFragmentShaderId);
    glAttachShader(objectShaderProgram, objectFragmentShaderId);

    // Use the shader program
    glLinkProgram(objectShaderProgram);
    glUseProgram(objectShaderProgram);

    // Delete shaders once linked
    glDeleteShader(objectVertexShaderId);
    glDeleteShader(objectFragmentShaderId);
}

/**
 * Create textures
 */
void CreateTextures() {
    // Bind texture
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);

    // Load texture
    int width;
    int height;
    unsigned char* image = SOIL_load_image(TEXTURE_FILE, &width, &height, 0, SOIL_LOAD_RGBA);
    if (!image) {
        fprintf(stderr, "Failed to load texture %s", TEXTURE_FILE);
        exit(EXIT_FAILURE);
    }
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
    glGenerateMipmap(GL_TEXTURE_2D);
    SOIL_free_image_data(image);

    // Unbind texture
    glBindTexture(GL_TEXTURE_2D, 0);
}

/**
 * Convert a planar rectangle to a set of triangles
 *
 * @param axis The axis perpendicular to the rectangle
 * @param rectArray Array of rectangle data
 * @param triArray Array of triangle data
 * @param rectIndex The index of the rectangle to copy
 * @param rectIndex The index of the triangle output
 */
void PlanarRectangleToTriangles(char axis, GLfloat* rectArray, GLfloat* triArray, int rectIndex, int triIndex) {
    int x1, x2, y1, y2, y3, y4, z1, z2;
    // rectX format
    if (axis == 'x') {
        x1 = 0;
        x2 = 0;
        y1 = 1;
        y2 = 3;
        y3 = 3;
        y4 = 1;
        z1 = 2;
        z2 = 4;
    }
    // rectY format
    if (axis == 'y') {
        x1 = 1;
        x2 = 3;
        y1 = 0;
        y2 = 0;
        y3 = 0;
        y4 = 0;
        z1 = 2;
        z2 = 4;
    }
    // rectZ format
    if (axis == 'z') {
        x1 = 1;
        x2 = 3;
        y1 = 2;
        y2 = 4;
        y3 = 2;
        y4 = 4;
        z1 = 0;
        z2 = 0;
    }

    // Triangle 1, vertex 1
    triArray[triIndex + 0] =  rectArray[rectIndex + x1];
    triArray[triIndex + 1] =  rectArray[rectIndex + y1];
    triArray[triIndex + 2] =  rectArray[rectIndex + z1];
    // Triangle 1, vertex 2
    triArray[triIndex + 8] =  rectArray[rectIndex + x2];
    triArray[triIndex + 9] =  rectArray[rectIndex + y3];
    triArray[triIndex + 10] =  rectArray[rectIndex + z1];
    // Triangle 1, vertex 3
    triArray[triIndex + 16] = rectArray[rectIndex + x2];
    triArray[triIndex + 17] = rectArray[rectIndex + y2];
    triArray[triIndex + 18] = rectArray[rectIndex + z2];

    // Triangle 2, vertex 1
    triArray[triIndex + 24] = rectArray[rectIndex + x1];
    triArray[triIndex + 25] = rectArray[rectIndex + y1];
    triArray[triIndex + 26] = rectArray[rectIndex + z1];
    // Triangle 3, vertex 2
    triArray[triIndex + 32] = rectArray[rectIndex + x1];
    triArray[triIndex + 33] = rectArray[rectIndex + y4];
    triArray[triIndex + 34] = rectArray[rectIndex + z2];
    // Triangle 4, vertex 3
    triArray[triIndex + 40] = rectArray[rectIndex + x2];
    triArray[triIndex + 41] = rectArray[rectIndex + y2];
    triArray[triIndex + 42] = rectArray[rectIndex + z2];

    // Texture
    triArray[triIndex + 3] = 0;
    triArray[triIndex + 4] = 0;
    triArray[triIndex + 11] = 1;
    triArray[triIndex + 12] = 0;
    triArray[triIndex + 19] = 1;
    triArray[triIndex + 20] = 1;
    triArray[triIndex + 27] = 0;
    triArray[triIndex + 28] = 0;
    triArray[triIndex + 35] = 0;
    triArray[triIndex + 36] = 1;
    triArray[triIndex + 43] = 1;
    triArray[triIndex + 44] = 1;

    // Normals
    // Direction is copied from the direction of the rectangle
    float direction = rectArray[rectIndex + 4] - rectArray[rectIndex + 2];
    for (int i = 0; i < 6; i++) {
        triArray[triIndex + i*8 + 5] = (axis == 'x') ? direction : 0;
        triArray[triIndex + i*8 + 6] = (axis == 'y') ? direction : 0;
        triArray[triIndex + i*8 + 7] = (axis == 'z') ? direction : 0;
    }
}

/**
 * Create buffer objects
 */
void CreateBuffers() {
    // Number of elements in each row of the arrays
    int rectRowSize = 5;
    int vertexRowSize = 6 * 8;

    // Number of rectangles in each array
    int rectXSize = sizeof(rectX) / rectRowSize / sizeof(GLfloat);
    int rectYSize = sizeof(rectY) / rectRowSize / sizeof(GLfloat);
    int rectZSize = sizeof(rectZ) / rectRowSize / sizeof(GLfloat);

    // Allocate memory for the vertex data
    int offsetX = 0;
    int offsetY = offsetX + rectXSize * vertexRowSize;
    int offsetZ = offsetY + rectYSize * vertexRowSize;
    numVertices = offsetZ + rectZSize * vertexRowSize;
    GLfloat vertices[numVertices];

    // Convert rectangles into triangles and copy them to the vertex data
    for (int r = 0; r < rectXSize; r++) {
        PlanarRectangleToTriangles('x', rectX, vertices, r * rectRowSize, offsetX + r * vertexRowSize);
    }
    for (int r = 0; r < rectYSize; r++) {
        PlanarRectangleToTriangles('y', rectY, vertices, r * rectRowSize, offsetY + r * vertexRowSize);
    }
    for (int r = 0; r < rectZSize; r++) {
        PlanarRectangleToTriangles('z', rectZ, vertices, r * rectRowSize, offsetZ + r * vertexRowSize);
    }

    // Activate the vertex array
    glGenVertexArrays(1, &VAO);
    glBindVertexArray(VAO);

    // Create vertex buffer
    glGenBuffers(1, &VBO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

    // Create position pointer
    GLuint floatsPerPosition = 3;
    GLint positionStride = sizeof(float) * 8;
    size_t positionOffset = 0;
    glVertexAttribPointer(0, floatsPerPosition, GL_FLOAT, GL_FALSE, positionStride, (GLvoid*)positionOffset);
    glEnableVertexAttribArray(0);

    // Create texture pointer
    GLuint floatsPerTexture = 2;
    GLint textureStride = sizeof(float) * 8;
    size_t textureOffset = sizeof(float) * 3;
    glVertexAttribPointer(1, floatsPerTexture, GL_FLOAT, GL_FALSE, textureStride, (GLvoid*)textureOffset);
    glEnableVertexAttribArray(1);

    // Create normal pointer
    GLuint floatsPerNormal = 3;
    GLint normalStride = sizeof(float) * 8;
    size_t normalOffset = sizeof(float) * 5;
    glVertexAttribPointer(2, floatsPerNormal, GL_FLOAT, GL_FALSE, normalStride, (GLvoid*)normalOffset);
    glEnableVertexAttribArray(2);

    // Deactivate the vertex array
    glBindVertexArray(0);
}

/**
 * Destroy buffer objects
 */
void DestroyBuffers() {
    glDeleteVertexArrays(1, &VAO);
    glDeleteBuffers(1, &VBO);
}
