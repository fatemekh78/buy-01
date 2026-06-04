# Buy-01: E-Commerce Microservices Platform

A complete, production-ready e-commerce platform featuring Spring Boot microservices, Angular frontend, Jenkins CI/CD, and SonarQube code quality integration.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running the Application](#running-the-application)
- [Jenkins CI/CD Pipeline](#jenkins-cicd-pipeline)
- [SonarQube Code Quality](#sonarqube-code-quality)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture Overview

The Buy-01 platform is a three-tier distributed system:

### **Layer 1: Core E-Commerce Platform (Buy-01)**

The foundational microservices architecture with the following components:

```
┌─────────────────────────────────────────────────────┐
│                  Angular Frontend                   │
│              (Port 4200, HTTPS Enabled)             │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────┐
│          API Gateway (Spring Cloud Gateway)          │
│          Port 8443 (HTTPS), Routes & Auth            │
└──────┬──────────┬──────────┬──────────┬──────────────┘
       │          │          │          │
┌──────▼───┐ ┌────▼────┐ ┌───▼────┐ ┌──▼──────┐
│ Discovery│ │User     │ │Product │ │ Media   │
│ Service  │ │Service  │ │Service │ │ Service │
│(8761)    │ │(8081)   │ │(8082)  │ │(8083)   │
└──────────┘ └─────────┘ └────────┘ └─────────┘
             │                              │
     ┌───────▼───────────────────────────┬──▼────────┐
     │     MongoDB (NoSQL Database)      │  Kafka    │
     │     Port 27017, Replicas Ready    │(Async Msg)│
     └───────────────────────────────────┴───────────┘

Additional Services:
- Zookeeper (Kafka coordination)
- SonarQube (Code Quality, Port 9000)
```

### **Layer 2: Jenkins CI/CD Pipeline (MR-Jenk)**

Automated continuous integration and deployment:

- **Triggers**: GitHub push events
- **Stages**: Checkout → Build → Test → SonarQube → Docker Push → Deploy → Notify
- **Agents**: Distributed build support
- **Deployment**: Local Docker or SSH-based

### **Layer 3: SonarQube Code Quality (SafeZone)**

Continuous code quality monitoring:

- Static code analysis
- Security vulnerability detection
- Technical debt tracking
- GitHub integration via webhooks

---

## 📦 Prerequisites

### System Requirements

- **OS**: macOS, Linux, or Windows (with WSL2)
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk**: 20GB free space
- **CPU**: 4+ cores

### Required Software

```bash
# Core tools
- Docker Desktop (with Docker Compose)  # https://docs.docker.com/desktop/
- Git                                   # https://git-scm.com/
- Java 17+ (for local development)      # https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- Maven 3.9+ (for local builds)         # https://maven.apache.org/
- Node.js 18+ & npm (for frontend)      # https://nodejs.org/

# Optional (for local Jenkins/SonarQube development)
- Jenkins (for CI/CD testing)           # https://www.jenkins.io/
- SonarQube Community Edition           # https://www.sonarqube.org/
```

### Installation Check

```bash
# Verify installations
docker --version
docker-compose --version
git --version
java -version
mvn --version
npm --version
```

---

## 🚀 Quick Start

### 1. Clone and Navigate to Project

```bash
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01
```

### 2. Configure Environment (Optional but Recommended)

```bash
# Create .env file for Docker Compose overrides
cat > .env << 'EOF'
IMAGE_TAG=latest
DOCKER_REPO=mahdikheirkhah
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=password
EOF
```

### 3. Run Everything with One Command

```bash
# Option A: Using Makefile (Recommended - FIXED!)
make all

# This runs:
# 1. make build     - Builds all Java services and Angular frontend using their individual Dockerfiles
# 2. make up        - Starts all services with docker-compose

# Output should look like:
# --- Building Java Microservices ---
# Building api-gateway...
# Building user-service...
# Building product-service...
# Building media-service...
# Building discovery-service...
# Building dummy-data...
# --- Building Angular Frontend ---
# --- Starting Docker Compose Services ---
```

**Note**: The Makefile was just fixed! It now correctly uses each service's individual `Dockerfile` (e.g., `backend/api-gateway/Dockerfile`) instead of looking for a non-existent `Dockerfile.java`.

### 4. Verify Services Are Running

```bash
# Check container status
docker ps

# Expected output:
# - discovery-service (8761)
# - api-gateway (8443)
# - user-service (8081)
# - product-service (8082)
# - media-service (8083)
# - frontend (4200)
# - kafka, zookeeper, mongo, sonarqube
```

### 5. Access Applications

**Local Access:**

- **Frontend**: https://localhost:4200
- **API Gateway**: https://localhost:8443/actuator/health
- **Eureka Discovery**: http://localhost:8761
- **SonarQube**: http://localhost:9000 (admin/admin)

**External Access with ngrok:**

If you need to access your services from outside your network (e.g., testing on mobile, sharing with team):

```bash
# Start with ngrok tunnels
./setup.sh --ngrok

# Or start everything including Jenkins and ngrok
./setup.sh --jenkins --ngrok
```

After starting with `--ngrok`:

- Check the ngrok dashboard: http://localhost:4040
- Frontend will be accessible via: `https://[random-name].ngrok-free.app`
- Jenkins (if enabled) will be accessible via: `https://[random-name].ngrok-free.app`

**Note**: You need to install and authenticate ngrok first:

```bash
# Install ngrok
brew install ngrok/ngrok/ngrok          # macOS
snap install ngrok                       # Linux
# Or download from: https://ngrok.com/download

# Authenticate (get token from https://dashboard.ngrok.com/)
ngrok config add-authtoken <your-token>
```

---

## ⚠️ Recent Fixes Applied

### Makefile Build Fix (January 5, 2026)

**Problem**: `make all` was failing with error: `failed to read dockerfile: open Dockerfile.java: no such file or directory`

**Root Cause**: The original Makefile was looking for a non-existent generic `Dockerfile.java` in the root directory.

**Solution Applied**:

1. ✅ Updated Makefile to use each service's **individual Dockerfile** in its directory
2. ✅ Updated `docker-compose.yml` to use default `IMAGE_TAG:-latest` values
3. ✅ Added `backend/discovery-service` to the build list

**Changes Made**:

```makefile
# BEFORE (broken):
docker build --file Dockerfile.java --tag backend/user-service ...

# AFTER (fixed):
docker build --file backend/user-service/Dockerfile --tag mahdikheirkhah/user-service:latest ...
```

**Result**: `make all` now works correctly! ✅

---

## 🐳 Docker Files & Commands Reference

### Understanding the Docker Files in the Project

Your project uses **three Docker configurations**:

#### 1. **Dockerfile** (Root Directory)

**Purpose**: Generic multi-stage builder for Java microservices  
**Used by**: Makefile to build API Gateway, User Service, Product Service, Media Service, Discovery Service

```bash
# Build a single service using this Dockerfile
docker build \
  --file Dockerfile \
  --tag backend/user-service \
  --build-arg SERVICE_NAME=user-service \
  .
```

#### 2. **Dockerfile.jenkins** (Root Directory)

**Purpose**: Jenkins agent/slave image for running CI/CD pipelines  
**Used by**: Jenkins container in docker-compose.jenkins.yml

```bash
# Build Jenkins agent image
docker build -f Dockerfile.jenkins -t jenkins-agent .
```

#### 3. **docker-compose.yml** (Root Directory)

**Purpose**: Orchestrates all microservices + infrastructure for LOCAL DEVELOPMENT  
**Starts**: User Service, Product Service, Media Service, Discovery Service, API Gateway, Frontend, MongoDB, Kafka, Zookeeper, SonarQube

```bash
# Start all services
docker-compose up -d

# View all running containers
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

#### 4. **docker-compose.jenkins.yml** (Root Directory)

**Purpose**: Deploys the full application + Jenkins for CI/CD pipeline testing  
**Starts**: All microservices (as pre-built images) + Jenkins  
**Used for**: Production-like deployments or Jenkins testing environment

```bash
# Start with Jenkins integration
docker-compose -f docker-compose.jenkins.yml up -d

# View Jenkins logs
docker-compose -f docker-compose.jenkins.yml logs -f jenkins

# Stop
docker-compose -f docker-compose.jenkins.yml down
```

#### 5. **Dockerfile** (frontend/, backend/\*/Dockerfile)

**Purpose**: Individual service Dockerfiles for building service-specific images  
**Examples**:

- `frontend/Dockerfile` - Builds Angular frontend with Nginx
- `backend/api-gateway/Dockerfile` - Builds API Gateway service
- `backend/user-service/Dockerfile` - Builds User Service
- `backend/media-service/Dockerfile` - Builds Media Service
- etc.

---

### Quick Docker Commands

#### Build Docker Images

**Using Makefile (Recommended - NOW FIXED!)**

```bash
# Build all Java services + Frontend (uses individual Dockerfiles)
make build

# This builds:
# - backend/api-gateway using backend/api-gateway/Dockerfile
# - backend/user-service using backend/user-service/Dockerfile
# - backend/product-service using backend/product-service/Dockerfile
# - backend/media-service using backend/media-service/Dockerfile
# - backend/discovery-service using backend/discovery-service/Dockerfile
# - backend/dummy-data using backend/dummy-data/Dockerfile
# - frontend/angular app using frontend/Dockerfile

# Image tags created: mahdikheirkhah/service-name:latest
```

**Manual Docker Build Commands**

```bash
# Build a specific service (using its individual Dockerfile)
docker build \
  --file backend/api-gateway/Dockerfile \
  --tag mahdikheirkhah/api-gateway:latest \
  backend/api-gateway/

# Build frontend
docker build \
  --file frontend/Dockerfile \
  --tag mahdikheirkhah/frontend:latest \
  frontend/

# Build with specific tag/version
docker build \
  --file backend/user-service/Dockerfile \
  --tag mahdikheirkhah/user-service:v1.0.0 \
  backend/user-service/

# Build with no cache (fresh build)
docker build --no-cache -f backend/api-gateway/Dockerfile -t mahdikheirkhah/api-gateway backend/api-gateway/
```

#### Start Services

**Using Makefile**

```bash
# Start all services
make up

# Or manually with docker-compose
docker-compose up -d
```

**Using docker-compose directly**

```bash
# Start services in background
docker-compose up -d

# Start with logs visible (foreground)
docker-compose up

# Start only specific services
docker-compose up -d api-gateway user-service mongo

# Start with rebuild (if images changed)
docker-compose up -d --build
```

#### Manage Services

```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# View container logs
docker logs container-name
docker logs -f container-name  # Follow logs (Ctrl+C to exit)

# Execute command in running container
docker exec -it container-name bash
docker exec -it mongo mongosh  # Access MongoDB shell

# Stop services
docker-compose stop

# Stop and remove services
docker-compose down

# Remove volumes too (data loss!)
docker-compose down -v

# Restart services
docker-compose restart
docker-compose restart api-gateway
```

#### Debugging

```bash
# Check service health
curl -k https://localhost:8443/actuator/health
curl http://localhost:8761/actuator/health

# View container resource usage
docker stats

# Inspect container details
docker inspect container-name

# View container events in real-time
docker events

# Check network connectivity
docker-compose exec api-gateway ping user-service
```

#### Clean Up

```bash
# Stop and remove containers
docker-compose down

# Remove all stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Complete cleanup (containers, images, volumes, networks)
docker system prune -a --volumes

# Remove specific image
docker rmi backend/api-gateway
```

---

## ❓ Is SonarQube Necessary?

### **SHORT ANSWER**: ❌ **No, SonarQube is NOT required to run the application**

### Detailed Breakdown:

| Component          | Required?       | Purpose                                 | When to Use                              |
| ------------------ | --------------- | --------------------------------------- | ---------------------------------------- |
| **Docker Compose** | ✅ **YES**      | Runs all microservices & infrastructure | Always (unless running services locally) |
| **MongoDB**        | ✅ **YES**      | Database for storing data               | Always                                   |
| **Kafka**          | ⚠️ **Optional** | Async messaging between services        | Production / Advanced features           |
| **SonarQube**      | ❌ **NO**       | Code quality analysis (non-functional)  | Only if you want code quality metrics    |
| **Jenkins**        | ❌ **NO**       | CI/CD automation (non-functional)       | Only if you want automated pipelines     |

### What Happens If You Skip SonarQube?

✅ **Application will work perfectly fine without it:**

- All microservices run normally
- Database operations work
- API endpoints respond
- Frontend loads and functions
- Users can register, login, create products, upload media

❌ **What you lose without SonarQube:**

- Code quality metrics
- Vulnerability detection
- Technical debt tracking
- Code smell identification
- Security hotspot reporting

### When You NEED SonarQube:

1. **Enterprise/Corporate Environments** - Mandatory code quality gates
2. **Security-Critical Applications** - Must identify vulnerabilities
3. **Large Teams** - Track code quality across team
4. **Compliance Requirements** - Regulatory standards (HIPAA, SOC2, etc.)
5. **Long-term Projects** - Monitor technical debt over time

### When You DON'T Need SonarQube:

1. **Learning/Educational Projects** - Like this one! Focus on features first
2. **Prototypes & MVPs** - Speed matters more than code quality
3. **Small Projects** - Manual code reviews sufficient
4. **Development Environments** - Run it locally only when needed

---

### Recommended Running Scenarios:

#### **Scenario 1: Just Learn the Application (Recommended for beginners)**

```bash
# Run WITHOUT SonarQube - Focus on features
make all

# This starts:
# ✅ All microservices
# ✅ Database & Kafka
# ✅ Frontend
# ❌ SonarQube (skipped - not needed)
```

**What to do**: Use the application, test APIs, explore the code. Come back to SonarQube later.

---

#### **Scenario 2: Want Code Quality Analysis**

```bash
# Run WITH SonarQube - Complete setup
make all

# SonarQube starts automatically on port 9000
# Access: http://localhost:9000 (admin/admin)

# Then analyze code:
cd backend && mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin
```

---

#### **Scenario 3: Only Use Jenkins (for CI/CD)**

```bash
# Run Jenkins environment WITHOUT full SonarQube integration
docker-compose -f docker-compose.jenkins.yml up -d

# This starts:
# ✅ All microservices (pre-built)
# ✅ Jenkins (CI/CD)
# ⚠️ SonarQube (included but optional)

# Access Jenkins: http://localhost:8080
```

---

### How to Skip SonarQube If It's Running

If you've already started services with `make up` and want to disable SonarQube:

**Option 1: Stop only SonarQube**

```bash
docker-compose stop sonarqube
docker-compose rm sonarqube  # Remove container
```

**Option 2: Edit docker-compose.yml**

```bash
# Comment out the sonarqube section in docker-compose.yml
# Then restart
docker-compose down
docker-compose up -d
```

**Option 3: Start services without SonarQube**

```bash
# Start all EXCEPT sonarqube
docker-compose up -d \
  kafka zookeeper mongo \
  discovery-service api-gateway \
  user-service product-service media-service \
  frontend
```

---

### Summary Table

```
┌─────────────────────┬──────────────┬──────────────────────────────┐
│ Component           │ Requirement  │ Run Command                  │
├─────────────────────┼──────────────┼──────────────────────────────┤
│ Core App            │ ✅ REQUIRED  │ make all                     │
│ Microservices       │ ✅ REQUIRED  │ (started by docker-compose)  │
│ Database (MongoDB)  │ ✅ REQUIRED  │ (started automatically)       │
│ Message Broker      │ ⚠️ Optional  │ (started by default)         │
│ SonarQube           │ ❌ Optional  │ Stop it if not needed        │
│ Jenkins             │ ❌ Optional  │ docker-compose.jenkins.yml   │
└─────────────────────┴──────────────┴──────────────────────────────┘
```

---

## 🏃 Running the Application

### Option 1: Automated Setup Script (Easiest)

Use the `setup.sh` script for one-command startup:

```bash
# Basic startup (core services only)
./setup.sh

# Clean Docker and start fresh
./setup.sh --clean

# Start with Jenkins CI/CD
./setup.sh --jenkins

# Start with ngrok for external access
./setup.sh --ngrok

# Complete setup: clean + Jenkins + ngrok
./setup.sh --clean --jenkins --ngrok
```

**What the script does:**

- ✅ Validates Docker is running
- ✅ Optionally cleans Docker (with `--clean`)
- ✅ Starts all microservices and infrastructure
- ✅ Optionally starts Jenkins (with `--jenkins`)
- ✅ Optionally starts ngrok tunnels (with `--ngrok`)
- ✅ Displays all access URLs and credentials
- ✅ Shows useful commands for monitoring

### Option 2: Docker Compose (Manual Control)

#### Start All Services

```bash
# Build all images (first time or after code changes)
make build

# Start services in background
make up

# View logs (all services)
docker-compose logs -f

# View logs for specific service
docker-compose logs -f api-gateway
docker-compose logs -f user-service
docker-compose logs -f product-service
docker-compose logs -f media-service
docker-compose logs -f frontend
```

#### Stop and Clean Up

```bash
# Stop all services
make down

# Stop without removing volumes
docker-compose stop

# Restart services
docker-compose restart

# Complete cleanup (removes volumes, networks, etc.)
make clean
```

### Option 3: Local Development (Java Backend + Frontend)

#### Terminal 1: Start Backend Services

```bash
# Navigate to backend
cd backend

# Build parent project
mvn clean install

# Start Discovery Service
cd discovery-service
mvn spring-boot:run

# In another terminal, start other services
cd ../user-service && mvn spring-boot:run
cd ../product-service && mvn spring-boot:run
cd ../media-service && mvn spring-boot:run
```

#### Terminal 2: Start API Gateway

```bash
cd backend/api-gateway
mvn spring-boot:run
```

#### Terminal 3: Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm start

# Access at http://localhost:4200
```

### Option 3: Using Individual Docker Compose Files

#### For Jenkins Deployment Environment

```bash
# This uses the Jenkins-specific configuration
docker-compose -f docker-compose.jenkins.yml up -d

# View Jenkins logs
docker-compose -f docker-compose.jenkins.yml logs -f jenkins

# Stop
docker-compose -f docker-compose.jenkins.yml down
```

---

## 🔄 Jenkins CI/CD Pipeline

### Setting Up Jenkins

#### Prerequisites

- Docker installed
- GitHub repository access
- Docker Hub account (for image registry)

#### Method 1: Docker-Based Jenkins (Recommended)

```bash
# Start Jenkins using docker-compose.jenkins.yml
docker-compose -f docker-compose.jenkins.yml up -d

# Retrieve initial admin password
docker-compose -f docker-compose.jenkins.yml exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Access Jenkins
# URL: http://localhost:8080
# Username: admin
# Password: [paste the password from above]
```

#### Method 2: Manual Jenkins Installation

```bash
# Download and install Jenkins (macOS with Homebrew)
brew install jenkins-lts
brew services start jenkins-lts

# Access Jenkins at http://localhost:8080
```

### Configuring the Pipeline

#### Step 1: Create Credentials in Jenkins

1. Go to **Manage Jenkins** → **Manage Credentials** → **System** → **Global credentials**
2. Add the following credentials:

   **Docker Hub Credentials**
   - Kind: Username with password
   - Username: `[your-docker-hub-username]`
   - Password: `[your-docker-hub-token]`
   - ID: `dockerhub-credentials`

   **GitHub Credentials** (for repo access)
   - Kind: Username with password
   - Username: `[your-github-username]`
   - Password: `[your-github-personal-access-token]`
   - ID: `github-credentials`

   **SonarQube Credentials** (optional)
   - Kind: Secret text
   - Secret: `[your-sonarqube-token]`
   - ID: `sonarqube-token`

#### Step 2: Create a New Pipeline Job

1. Go to **New Item**
2. Enter job name: `Buy-01-Pipeline`
3. Select: **Pipeline**
4. Configure:

   **Definition**: Pipeline script from SCM

   **SCM**: Git
   - Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
   - Credentials: Select `github-credentials`
   - Branch: `*/main`
   - Script path: `Jenkinsfile`

5. **Save**

#### Step 3: Configure Build Triggers

1. In job configuration, check **GitHub hook trigger for GITScm polling**
2. In GitHub Settings:
   - Go to **Settings** → **Webhooks** → **Add webhook**
   - Payload URL: `http://[your-jenkins-ip]:8080/github-webhook/`
   - Content type: `application/json`
   - Events: Push events

### Running the Pipeline

#### Manual Trigger

```bash
# Option 1: Via Jenkins UI
1. Click on "Build Now" in job page

# Option 2: Via curl
curl -X POST http://localhost:8080/job/Buy-01-Pipeline/build \
  --user admin:${JENKINS_API_TOKEN}
```

#### Parameters

The pipeline supports customizable parameters:

```groovy
BRANCH              // Git branch to build (default: main)
RUN_TESTS           // Execute unit tests (default: true)
RUN_INTEGRATION_TESTS // Execute integration tests (default: false)
RUN_SONAR           // Run SonarQube analysis (default: true)
SKIP_DEPLOY         // Skip deployment step (default: true)
DEPLOY_LOCALLY      // Deploy via Docker Compose (default: true)
```

Example: Build with custom parameters via UI or CLI

```bash
# Build specific branch with SonarQube enabled
curl -X POST http://localhost:8080/job/Buy-01-Pipeline/buildWithParameters \
  -F BRANCH=develop \
  -F RUN_SONAR=true \
  -F DEPLOY_LOCALLY=true \
  --user admin:${JENKINS_API_TOKEN}
```

### Pipeline Stages Explained

| Stage              | Purpose    | Details                                   |
| ------------------ | ---------- | ----------------------------------------- |
| **Initialization** | Setup      | Logs build parameters and environment     |
| **Checkout**       | SCM        | Pulls latest code from Git branch         |
| **Backend Build**  | Compile    | Builds Java services with Maven           |
| **Backend Tests**  | QA         | Runs JUnit tests (if `RUN_TESTS=true`)    |
| **Frontend Build** | Compile    | Builds Angular app with npm               |
| **Frontend Tests** | QA         | Runs Jasmine/Karma tests                  |
| **SonarQube**      | Analysis   | Publishes code quality metrics            |
| **Docker Build**   | Package    | Creates container images for each service |
| **Docker Push**    | Registry   | Pushes images to Docker Hub               |
| **Deploy**         | Deployment | Deploys via Docker Compose (local or SSH) |
| **Notifications**  | Alerts     | Sends Slack/Email notifications           |

---

## 📊 SonarQube Code Quality

### Setting Up SonarQube

#### Method 1: Via Docker Compose (Included)

SonarQube is automatically started when you run `make up`. No additional setup needed.

```bash
# SonarQube is running at http://localhost:9000
# Default credentials: admin / admin
```

#### Method 2: Manual Docker

```bash
# Pull and run SonarQube
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_logs:/opt/sonarqube/logs \
  sonarqube:lts-community
```

### Configuring Projects in SonarQube

#### Step 1: Create Organization and Project

1. Access http://localhost:9000
2. Login with **admin** / **admin**
3. Click **Create** → **New organization**
   - Organization key: `buy-01`
   - Organization name: `Buy-01 E-Commerce`

#### Step 2: Create Projects

For each module (Backend, Frontend), create separate projects:

```
Projects to create:
- buy-01-backend (Java)
- buy-01-frontend (TypeScript/Angular)
```

#### Step 3: Generate Project Tokens

1. Go to **Administration** → **Security** → **Tokens**
2. Generate tokens for:
   - `buy-01-backend`
   - `buy-01-frontend`
3. Copy tokens and store securely (for Jenkins)

### Running Code Analysis

#### From Jenkins Pipeline (Automated)

The pipeline automatically runs SonarQube when `RUN_SONAR=true`:

```groovy
stage('📊 SonarQube Analysis') {
    when { expression { params.RUN_SONAR } }
    steps {
        withSonarQubeEnv('SonarQube') {
            sh '''
                mvn clean verify sonar:sonar \
                  -Dsonar.projectKey=buy-01-backend
            '''
        }
    }
}
```

#### Manual Local Analysis

**Backend (Java)**

```bash
cd backend

# Run analysis with Maven
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=[your-token]
```

**Frontend (Angular/TypeScript)**

```bash
cd frontend

# Install SonarScanner for JS/TS
npm install -D sonar-scanner

# Run analysis
./node_modules/.bin/sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.sources=src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=[your-token]
```

### Viewing Results and Reports

1. **Dashboard**: http://localhost:9000/dashboard
   - Overview of all projects
   - Code coverage metrics
   - Security hotspots

2. **Issue Tracking**
   - View bugs, code smells, vulnerabilities
   - Filter by severity (Blocker, Critical, Major, Minor, Info)
   - Assign to developers

3. **Quality Gates**
   - Define pass/fail criteria
   - Block releases if standards not met
   - Gate: Coverage > 80%, Rating A+

### GitHub Integration

#### Webhooks and Pull Request Analysis

```bash
# In SonarQube (Administration → General Settings → GitHub):
1. Configure GitHub App:
   - Organization: mahdikheirkhah
   - Repository: buy-01
   - Generate app credentials

2. PR Decoration:
   - Automatic comments on PRs with analysis results
   - Report quality gates status
```

---

## 📚 API Documentation

### Base URLs

- **Local Development**: `http://localhost:8443`
- **Production**: `https://api.buy-01.com`

### Authentication

JWT tokens are stored in **HTTP-only secure cookies** for enhanced security. The browser automatically sends the cookie with each request, so no manual `Authorization` header is needed.

**How it works:**

- On successful login/register, the server sets an HTTP-only cookie containing the JWT
- The cookie is automatically included in subsequent requests by the browser
- This prevents XSS attacks from accessing the token via JavaScript

```bash
# Cookie is set automatically by the server after login:
Set-Cookie: jwt=eyJhbGc...; HttpOnly; Secure; SameSite=Strict; Path=/
```

### Core Endpoints

#### User Service (Port 8081)

**Authentication**

```bash
# Register User
POST /auth/register
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "secure123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "SELLER" // or CLIENT
}

# Login
POST /auth/login
{
  "email": "user@example.com",
  "password": "secure123"
}
Response:
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600
}
# Note: Token is also set as HTTP-only cookie automatically
```

**User Profile**

```bash
# Get Current User
GET /api/users/me
Authorization: Bearer <token>

# Update Profile
PUT /api/users/me
{
  "firstName": "John",
  "lastName": "Doe"
}

# Upload Avatar (delegates to Media Service)
PUT /api/users/me/avatar
Content-Type: multipart/form-data
File: [image.jpg]
```

#### Product Service (Port 8082)

**Public Endpoints**

```bash
# List All Products
GET /api/products
Response:
[
  {
    "id": "507f1f77bcf86cd799439011",
    "name": "Product Name",
    "description": "...",
    "price": 99.99,
    "imageUrls": ["https://..."],
    "seller": { "id": "...", "name": "..." },
    "createdAt": "2026-01-05T10:00:00Z"
  }
]

# Get Product Details
GET /api/products/{id}
```

**Seller-Only Endpoints**

```bash
# Create Product (requires SELLER role)
POST /api/products
Authorization: Bearer <token>
{
  "name": "New Product",
  "description": "Description",
  "price": 199.99,
  "imageUrls": []
}

# Update Product (owner only)
PUT /api/products/{id}
{
  "name": "Updated Name",
  "price": 149.99
}

# Delete Product (owner only)
DELETE /api/products/{id}
```

#### Media Service (Port 8083)

**Upload Images**

```bash
# Upload Product Image (max 2MB)
POST /api/media/images
Authorization: Bearer <token>
Content-Type: multipart/form-data
File: [image.jpg]

Response:
{
  "id": "507f1f77bcf86cd799439012",
  "url": "https://localhost:8443/api/media/images/507f1f77bcf86cd799439012",
  "fileName": "image.jpg",
  "mimeType": "image/jpeg",
  "size": 150000,
  "createdAt": "2026-01-05T10:00:00Z"
}
```

**Download Images**

```bash
# Get Image
GET /api/media/images/{id}

# Get Thumbnail
GET /api/media/images/{id}/thumbnail
```

---

## 📁 Project Structure

```
buy-01/
├── backend/                           # Spring Boot Microservices
│   ├── pom.xml                       # Parent POM (multi-module)
│   ├── common/                       # Shared libraries, utilities, DTOs
│   │   ├── src/main/java/
│   │   │   └── com/buy01/
│   │   │       └── common/
│   │   │           ├── dto/
│   │   │           ├── exception/
│   │   │           ├── security/
│   │   │           ├── kafka/
│   │   │           └── util/
│   │   └── pom.xml
│   │
│   ├── discovery-service/            # Eureka Service Registry
│   │   ├── src/main/java/com/buy01/discovery/
│   │   ├── src/main/resources/application.properties
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── api-gateway/                  # Spring Cloud Gateway
│   │   ├── src/main/java/com/buy01/gateway/
│   │   │   ├── config/              # Gateway routing, security
│   │   │   └── filter/              # Auth, CORS, logging filters
│   │   ├── src/main/resources/application.properties
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── user-service/                # User Management & Authentication
│   │   ├── src/main/java/com/buy01/user/
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # MongoDB operations
│   │   │   ├── entity/              # Domain models
│   │   │   ├── security/            # JWT, Spring Security
│   │   │   └── exception/           # Error handling
│   │   ├── src/test/java/          # Unit & integration tests
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── product-service/             # Product Management
│   │   ├── src/main/java/com/buy01/product/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── event/              # Kafka event publishing
│   │   ├── src/test/java/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── media-service/               # Image Upload & Management
│   │   ├── src/main/java/com/buy01/media/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── validation/          # File type, size validation
│   │   │   └── storage/             # File system operations
│   │   ├── uploads/                 # Image storage directory
│   │   ├── src/test/java/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── dummy-data/                  # Data seeding service
│   │   ├── src/main/java/
│   │   └── pom.xml
│   │
│   ├── certificates/                # SSL/TLS Certificates
│   │   ├── generate-certificates.sh # Certificate generation script
│   │   ├── ca/
│   │   └── keystores/               # JKS keystores per service
│   │
│   └── target/                      # Maven build output
│
├── frontend/                         # Angular Single Page Application
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/               # Services, guards, interceptors
│   │   │   │   ├── services/
│   │   │   │   │   ├── auth.service.ts
│   │   │   │   │   ├── user.service.ts
│   │   │   │   │   ├── product.service.ts
│   │   │   │   │   └── media.service.ts
│   │   │   │   ├── guards/
│   │   │   │   │   ├── auth.guard.ts
│   │   │   │   │   └── role.guard.ts
│   │   │   │   └── interceptors/
│   │   │   │       ├── auth.interceptor.ts
│   │   │   │       └── error.interceptor.ts
│   │   │   │
│   │   │   ├── shared/             # Reusable components, pipes
│   │   │   │   ├── components/
│   │   │   │   ├── pipes/
│   │   │   │   └── directives/
│   │   │   │
│   │   │   ├── auth/               # Auth module (Login, Register)
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   └── auth-routing.module.ts
│   │   │   │
│   │   │   ├── dashboard/          # Seller dashboard
│   │   │   │   ├── products/       # Manage products
│   │   │   │   ├── media/          # Upload/manage images
│   │   │   │   └── dashboard-routing.module.ts
│   │   │   │
│   │   │   ├── products/           # Public product browsing
│   │   │   │   ├── list/
│   │   │   │   ├── detail/
│   │   │   │   └── products-routing.module.ts
│   │   │   │
│   │   │   └── app-routing.module.ts
│   │   │
│   │   ├── assets/                 # Static files
│   │   ├── index.html
│   │   ├── main.ts
│   │   ├── styles.scss             # Global styles
│   │   ├── custom-theme.scss       # Material theme customization
│   │   └── app.component.ts
│   │
│   ├── public/                      # Public static assets
│   ├── angular.json                 # Angular CLI config
│   ├── package.json                 # Dependencies
│   ├── tsconfig.json               # TypeScript config
│   ├── Dockerfile                  # Multi-stage build
│   ├── nginx.conf                  # Nginx reverse proxy
│   └── README.md
│
├── lib/                             # Custom libraries (if any)
├── scripts/                         # (Optional) Utility scripts
│
├── certs/                           # SSL certificates (Let's Encrypt)
│
├── Dockerfile                       # Multi-stage generic Java builder
├── Dockerfile.jenkins              # Jenkins agent image (if needed)
├── Makefile                        # Build automation
├── Jenkinsfile                     # CI/CD pipeline definition
├── docker-compose.yml              # Local development environment
├── docker-compose.jenkins.yml      # Jenkins deployment environment
│
└── README.md                        # This file
```

---

## 👨‍💻 Development Workflow

### Local Development Setup

#### 1. First-Time Setup

```bash
# Clone repository
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01

# Install Git hooks (optional)
git config core.hooksPath .github/hooks

# Create local environment file
cp .env.example .env
```

#### 2. Backend Development

**Build Parent Project**

```bash
cd backend
mvn clean install -DskipTests
```

**Running Individual Services**

```bash
# Terminal 1: Discovery Service
cd backend/discovery-service
mvn spring-boot:run

# Terminal 2: User Service
cd backend/user-service
mvn spring-boot:run

# Terminal 3: Product Service
cd backend/product-service
mvn spring-boot:run

# Terminal 4: Media Service
cd backend/media-service
mvn spring-boot:run

# Terminal 5: API Gateway
cd backend/api-gateway
mvn spring-boot:run
```

#### 3. Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Runs on http://localhost:4200
# Auto-reload on file changes
```

#### 4. Running Tests

**Backend Unit Tests**

```bash
cd backend
mvn test
```

**Backend Integration Tests**

```bash
mvn verify
```

**Frontend Unit Tests**

```bash
cd frontend
npm test
```

**Frontend End-to-End Tests**

```bash
npm run e2e
```

### Git Workflow

#### Creating Feature Branches

```bash
# Create and checkout feature branch
git checkout -b feature/user-authentication
# or
git checkout -b fix/login-bug

# Make changes...
git add .
git commit -m "feat: implement user authentication with JWT"

# Push to remote
git push origin feature/user-authentication

# Create Pull Request via GitHub UI
```

#### Branch Naming Convention

```
feature/<feature-name>    # New features
fix/<bug-name>           # Bug fixes
refactor/<area>          # Code refactoring
docs/<topic>             # Documentation
test/<area>              # Test additions
```

#### Commit Message Convention

```
feat: add new user registration endpoint
fix: resolve NPE in product listing
refactor: extract gateway routing config
docs: update API documentation
test: add unit tests for media upload validation
style: format code according to checkstyle
chore: update dependencies
```

### Code Quality Checks (Local)

Before pushing, run quality checks:

```bash
# Backend: CheckStyle & SpotBugs
cd backend
mvn checkstyle:check
mvn spotbugs:check

# Frontend: ESLint & Prettier
cd frontend
npm run lint
npm run format

# Backend: SonarQube Analysis (requires running SonarQube)
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
```

---

## 🔧 Troubleshooting

For detailed troubleshooting guides and solutions to common issues, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

---

## 📞 Support & Resources

### Documentation

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Angular Docs](https://angular.io/docs)
- [MongoDB Docs](https://docs.mongodb.com/)
- [Apache Kafka](https://kafka.apache.org/documentation/)

### Tools & Services

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [SonarQube Docs](https://docs.sonarqube.org/)
- [Docker Docs](https://docs.docker.com/)
- [GitHub Docs](https://docs.github.com/)

### Team Communication

- **Issues & Bugs**: Create GitHub Issues
- **Features**: Create GitHub Discussions
- **PRs**: Submit Pull Requests with description

---

## 📄 License & Contributors

**License**: MIT

**Contributors**:

- [Mohammad Mahdi Kheirkhah](https://github.com/mahdikheirkhah/)
- [Fatemeh Kheirkhah](https://github.com/fatemekh78)
- [Parisa Rahinmi](https://github.com/prahimi94)
- [Kateryna Ovsiienko](https://github.com/mavka1207)

---

## 🎯 Next Steps: 

1. **Follow the Quick Start** to get the application running
2. **Access the Frontend** at https://localhost:4200
3. **Register as a Seller** to test product creation
4. **Upload Products** and manage media
5. **Monitor Code Quality** via SonarQube at http://localhost:9000
6. **Run Jenkins Pipeline** to test CI/CD automation

---

