# Free Hosting Guide for Vaccination System

## Option 1: Render (Recommended for Backend)

### Backend Deployment (Render - Free)
1. **Push your code to GitHub**
   - Create a GitHub repository
   - Push the entire vaccination-system folder (excluding target/ and node_modules/)

2. **Deploy on Render**
   - Go to https://render.com and sign up with GitHub
   - Create a new "Web Service"
   - Connect your GitHub repository
   - Configure:
     - Build Command: `mvn package -DskipTests`
     - Start Command: `java -jar target/vaccination-system-1.0.0.jar`
   - Add Environment Variables:
     - `SPRING_PROFILES_ACTIVE` = `h2`
     - `JWT_SECRET` = your-secret-key
   - Click "Create Web Service"

### Frontend Deployment (Render - Static)
1. Build the frontend:
   
```
bash
   cd frontend
   npm run build
   
```

2. Deploy the `dist` folder to Render as a "Static Site"

---

## Option 2: Vercel (Frontend) + Render (Backend)

### Frontend on Vercel (Free)
1. Install Vercel CLI: `npm i -g vercel`
2. Go to frontend folder: `cd frontend`
3. Run: `vercel`
4. Follow prompts - it will auto-detect Vite/React
5. Your site will be deployed for free

### Backend on Render (see Option 1)

---

## Option 3: Railway (All-in-One)

### Deploy Full Stack on Railway
1. Go to https://railway.app
2. Sign up with GitHub
3. Create new project → "Deploy from GitHub repo"
4. Select your repository
5. Configure environment variables
6. Railway will build and deploy both frontend and backend

---

## Option 4: Cyclic (Free Tier)

1. Go to https://cyclic.sh
2. Connect your GitHub repository
3. Cyclic automatically detects Node.js/React apps
4. Deploys both frontend and API

---

## Option 5: Fly.io (Free Tier)

1. Install flyctl: `winget install flyctl`
2. Run: `fly launch`
3. Configure your app
4. Deploy with: `fly deploy`

---

## Recommended Approach for Free Hosting:

### Step 1: Backend on Render (Free)
- Free tier: 750 hours/month
- Auto-shuts down after 15 min of inactivity
- Wakes up on first request

### Step 2: Frontend on Vercel (Free)
- Unlimited bandwidth
- Global CDN
- Custom domain support

### Step 3: Update Frontend API URL
After deploying backend, update:
- `frontend/vite.config.js` - change proxy target to your Render URL
- OR update `frontend/src/api/axios.js` - change baseURL

---

## Quick Deploy Steps (Recommended):

### Backend (Render):
1. Create GitHub repo
2. Connect to Render → Deploy automatically
3. Note your backend URL (e.g., `https://your-app.onrender.com`)

### Frontend (Vercel):
1. Go to https://vercel.com
2. Import your GitHub repo (frontend folder)
3. Set output directory: `dist`
4. Add environment variable: `VITE_API_URL` = your-backend-url
5. Update axios.js to use this URL

---

## Database Note:
- Default uses H2 (in-memory) - data resets on restart
- For persistent data, add MySQL on Render (free tier)
- Or use JawsDB (MySQL on Heroku)

## Custom Domain:
- Both Vercel and Render support free custom domains
- Add domain in settings and update DNS records
