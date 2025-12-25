-- 用户流量风控 Lua 脚本（固定窗口限流）
-- KEYS[1]: 限流 key
-- ARGV[1]: 时间窗口（秒）
-- 返回值: 当前时间窗口内的请求次数

local key = KEYS[1]
local window = tonumber(ARGV[1])

local count = redis.call('INCR', key)

-- 第一次请求，设置过期时间
if count == 1 then
    redis.call('EXPIRE', key, window)
end

return count
