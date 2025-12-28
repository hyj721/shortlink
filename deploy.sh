#!/bin/bash

# 部署脚本：替换配置文件中的密码占位符，构建镜像，然后恢复原文件
# 用法: ./deploy.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 配置文件路径
CONFIG_FILES=(
    "project/src/main/resources/shardingsphere-config-docker.yml"
    "admin/src/main/resources/shardingsphere-config-docker.yml"
)

# 清理函数：恢复备份文件
cleanup() {
    echo ""
    echo "� 恢复原始配置文件..."
    for config_file in "${CONFIG_FILES[@]}"; do
        backup_file="${config_file}.bak"
        if [ -f "$backup_file" ]; then
            mv "$backup_file" "$config_file"
            echo "  ✅ 已恢复: $config_file"
        fi
    done
}

# 捕获退出信号，确保即使出错也能恢复文件
trap cleanup EXIT

echo "�📋 读取 .env 文件..."
if [ ! -f ".env" ]; then
    echo "❌ 错误: .env 文件不存在"
    exit 1
fi

# 读取 .env 文件中的 MYSQL_ROOT_PASSWORD
source .env
if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
    echo "❌ 错误: MYSQL_ROOT_PASSWORD 未设置"
    exit 1
fi

echo "� 备份配置文件..."
for config_file in "${CONFIG_FILES[@]}"; do
    if [ -f "$config_file" ]; then
        cp "$config_file" "${config_file}.bak"
        echo "  ✅ 已备份: $config_file"
    else
        echo "  ⚠️  文件不存在: $config_file"
    fi
done

echo "🔧 替换密码占位符..."
for config_file in "${CONFIG_FILES[@]}"; do
    if [ -f "$config_file" ]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s/\${MYSQL_PASSWORD}/${MYSQL_ROOT_PASSWORD}/g" "$config_file"
        else
            # Linux
            sed -i "s/\${MYSQL_PASSWORD}/${MYSQL_ROOT_PASSWORD}/g" "$config_file"
        fi
        echo "  ✅ 已替换: $config_file"
    fi
done

echo "🚀 构建并启动 Docker Compose..."
docker-compose down 2>/dev/null || true
docker-compose up --build -d

echo ""
echo "✅ 部署完成!"
echo ""
echo "📊 查看容器状态: docker-compose ps"
echo "📜 查看日志: docker-compose logs -f [服务名]"

# cleanup 会在脚本退出时自动执行（通过 trap）
