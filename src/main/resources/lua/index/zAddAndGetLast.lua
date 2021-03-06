--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:51
-- To change this template use File | Settings | File Templates.
--

local zCard = tonumber(redis.call('zcard', KEYS[1]))
local insertLen = #ARGV / 2
local limit = tonumber(KEYS[2])
if (zCard + insertLen > limit) then
    if (KEYS[3] == '0') then
        return 'f'
    end
    local dim = zCard + insertLen - limit
    redis.call('zremrangebyrank', KEYS[1], 0, dim - 1)
    if (insertLen > limit) then
        local tmpTable = {}
        for i = 1, limit do table.insert(tmpTable, tonumber(ARGV[i * 2 - 1])) table.insert(tmpTable, ARGV[i * 2]) end
        ARGV = tmpTable
    end
end
redis.call('zadd', KEYS[1], unpack(ARGV))
return { redis.call('zrange', KEYS[1], 0, 0), redis.call('zcard', KEYS[1]) }

