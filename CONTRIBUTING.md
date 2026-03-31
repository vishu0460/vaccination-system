# 🤝 Contributing Guide

Help make VaxZone better!

## 🛠 Development Workflow

1. **Fork & Clone**:
```bash
git clone https://github.com/yourusername/vaxzone.git
cd vaxzone
```

2. **Setup** (see [SETUP_GUIDE.md](./SETUP_GUIDE.md)):
```bash
cp .env.example .env  # Edit secrets
docker compose up -d  # Or manual backend/frontend
```

3. **Branch**:
```bash
git checkout -b feature/your-feature
```

## 📝 Code Standards

- **Java**: IntelliJ/Google style, 100-char lines
- **JSX**: Prettier/ESLint, 2-space indent
- **Commit**: Conventional (`feat: add X`, `fix: resolve Y`)

## 🧪 Testing

**Backend**:
```bash
cd backend
mvn clean test  # 200+ JUnit
```

**Frontend**:
```bash
cd frontend
npm test        # Vitest unit
npm run test:e2e # Playwright
```

100% coverage encouraged.

## 🔄 Pull Request

1. Update [CHANGELOG.md](./CHANGELOG.md)
2. Tests pass
3. No lint errors
4. [TODO.md](./TODO.md) progress

**Template**:
```
**What**: Brief desc

**Changes**:
- File1: ...
- File2: ...

**Tests**: Added/Updated

Closes #issue
```

## 🚫 Don'ts

- No secrets in code/PRs
- No breaking changes without migration/deprecation
- Update docs for new features

## 📚 Resources

- [SECURITY.md](./SECURITY.md)
- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- Issues: Label `good first issue`

Thanks! 🎉

