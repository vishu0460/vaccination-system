# Vaccination System Data Fix TODO

## Plan Steps:

- [x] 1. Update application-local.yml: Enable H2 console (enabled: true, path: /h2-console)
- [x] 2. Create data.sql from data-complete.sql content for auto SQL init
- [x] 3. Update DataInitializer.java: Add logging, exception handling, future dates
- [ ] 4. Restart application
- [ ] 5. Verify H2 console: http://localhost:8080/api/h2-console (JDBC: jdbc:h2:mem:vaccination_db, sa, '')
- [ ] 6. Test APIs with curl:
  - curl http://localhost:8080/api/public/centers
  - curl http://localhost:8080/api/public/drives
  - curl http://localhost:8080/api/public/news
- [ ] 7. Check tables have data: SELECT COUNT(*) FROM vaccination_centers; etc.
- [ ] 8. Update frontend if needed

**Completed Steps Will Be Marked [x]**
