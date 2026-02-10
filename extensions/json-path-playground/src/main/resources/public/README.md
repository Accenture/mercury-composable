# JSON-Path Playground

A lightweight React-based web application for testing and debugging JSON-Path queries in real-time via WebSocket connections.

## Features

- Real-time WebSocket communication for JSON-Path testing
- Command history with arrow key navigation
- Support for JSON and XML payloads
- Interactive console for monitoring messages
- Clean, modern UI with CSS Modules

## Prerequisites

- Node.js 18+ recommended
- npm or yarn package manager
- Java 17+ (for running the Spring Boot backend)
- Maven (for building the Spring Boot application)

## Architecture

This project consists of two parts:

1. **Spring Boot Backend** (port 8085) - Handles WebSocket connections, serves API endpoints, and serves static files in production
2. **React Frontend** (Vite dev server on port 3000 in development) - Provides the UI and proxies API/WebSocket requests to the backend

## Development Workflow

### Option 1: Development Mode (Recommended for Development)

This mode gives you hot module replacement and fast refresh for React development.

**Step 1: Start the Spring Boot backend**

From the project root (`/json-path-playground`):

```bash
# Build the project (if not already built)
mvn clean package

# Start the Spring Boot server on port 8085
java -jar target/json-path-playground-4.3.62.jar
```

The backend will be running at `http://localhost:8085`.

**Step 2: Start the Vite dev server**

In a separate terminal, navigate to the public folder:

```bash
cd src/main/resources/public

# Install dependencies (first time only)
npm install

# Start the Vite dev server
npm run dev
```

The React app will be available at `http://localhost:3000` with all API and WebSocket requests automatically proxied to the Spring Boot backend at port 8085.

### Option 2: Production Mode

This mode serves the built React application through the Spring Boot server.

**Step 1: Build the React application**

```bash
cd src/main/resources/public

# Install dependencies (if not already installed)
npm install

# Build the production bundle
npm run build
```

This creates a `dist` folder with optimized production files.

**Step 2: Copy the build output to the public folder**

```bash
# From the public directory
cp -r dist/* ../public/
# Or manually copy the contents of dist/ to src/main/resources/public/
```

**Step 3: Rebuild and run the Spring Boot application**

```bash
# From the project root
mvn clean package
java -jar target/json-path-playground-4.3.62.jar
```

The complete application will be available at `http://localhost:8085`.

## Integration with Maven Build

To automate the React build as part of your Maven build process, you can add the `frontend-maven-plugin` to your `pom.xml`. This will automatically build the React app when you run `mvn package`.

Example configuration:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>src/main/resources/public</workingDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
            <configuration>
                <nodeVersion>v20.11.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>npm build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-react-build</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/public</outputDirectory>
                <resources>
                    <resource>
                        <directory>src/main/resources/public/dist</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Proxy Configuration

The Vite dev server is configured to proxy the following endpoints to the Spring Boot backend (port 8085):

- `/ws/*` - WebSocket connections
- `/info/*` - Info endpoints
- `/health` - Health check endpoint
- `/env` - Environment endpoint
- `/api/*` - API endpoints

This allows seamless development without CORS issues.

## Project Structure

```
public/
├── src/
│   ├── components/
│   │   ├── JsonPathPlayground.jsx       # Main component
│   │   └── JsonPathPlayground.module.css # Component styles
│   ├── App.jsx                           # App root component
│   ├── main.jsx                          # Entry point
│   └── index.css                         # Global styles
├── dist/                                 # Production build output (generated)
├── index.html                            # HTML template
├── vite.config.js                        # Vite configuration
├── package.json                          # Dependencies and scripts
└── README.md                             # This file
```

## Technology Stack

- **React 19**: Latest React with modern features
- **Vite**: Fast build tool and dev server
- **CSS Modules**: Scoped styling without conflicts
- **WebSocket API**: Real-time bidirectional communication

## Best Practices

This project follows enterprise-ready best practices:

1. **Modern React**: Uses React 19 with hooks and functional components
2. **CSS Modules**: Scoped styles prevent naming conflicts
3. **ESLint Ready**: Project structure supports adding linting configuration
4. **Type-Safety Ready**: Can easily add TypeScript by renaming files to `.tsx`
5. **Vite**: Modern, fast build tool with excellent DX
6. **Clean Architecture**: Separation of concerns with component-based structure

## Integration with Build Systems

This example can be easily integrated into various enterprise setups:

- **Next.js**: Component can be imported and used in Next.js pages
- **Vite**: Already configured for Vite
- **Create React App**: Component is framework-agnostic
- **Custom Webpack**: Standard React component can be integrated

## Usage

1. Click "Start" to establish WebSocket connection
2. Enter commands in the command input field
3. Use arrow keys to navigate command history
4. Paste JSON/XML payload and use the `load` command to send it
5. Monitor real-time responses in the console

## Configuration

Update `vite.config.js` to modify:
- Server port
- Proxy settings
- Build output directory
- Source maps

## License

Part of the Mercury Composable open-source library.
