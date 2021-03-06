--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:57
-- To change this template use File | Settings | File Templates.
--

local limit = tonumber(ARGV[1])
local argvOffset = 2
local cnt = 0
for keysOffset = 1, #KEYS, 2 do
    local values = {}
    local key = KEYS[keysOffset]
    local endIdx = argvOffset + tonumber(KEYS[keysOffset + 1]) - 1
    for i = argvOffset, endIdx do
        table.insert(values, ARGV[i])
    end
    argvOffset = endIdx + 1
    cnt = cnt + redis.call('zadd', key, unpack(values))
    local zCard = redis.call('zcard', key)
    local dim = zCard - limit
    if (dim > 0) then
        redis.call('zremrangebyrank', key, 0, dim - 1)
    end
end
return cnt

