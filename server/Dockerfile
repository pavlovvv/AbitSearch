# Збірка: docker compose up --build (з кореня репозиторію)
FROM node:22-alpine
WORKDIR /app
COPY server/package.json server/package-lock.json ./
RUN npm ci --omit=dev
COPY server/src ./src
ENV DB_FILE=/data/abitsearch.db
VOLUME /data
EXPOSE 8080
CMD ["node", "--no-warnings=ExperimentalWarning", "src/index.js"]
