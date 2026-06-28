# ============================================================
# Stage 1: Build — Node.js + npm
# ============================================================
FROM node:20-alpine AS build

WORKDIR /app

# 先复制依赖配置，利用 Docker 层缓存
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .

# 生产构建，API 后端通过 nginx 反向代理，baseURL 设为 /api
ARG VITE_API_BASE_URL=/api
RUN npm run build

# ============================================================
# Stage 2: Runtime — nginx
# ============================================================
FROM nginx:alpine

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从 build 阶段复制编译产物
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
