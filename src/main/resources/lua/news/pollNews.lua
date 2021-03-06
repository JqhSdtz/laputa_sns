--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:55
-- To change this template use File | Settings | File Templates.
--

local from = redis.call('hget', KEYS[1], ARGV[1])
local totalList = {}
if (from == nil or from == false) then from = nil else from = tonumber(from) end
for i = 2, #KEYS do
    local postList
    if (from == nil) then
        postList = redis.call('zrevrange', KEYS[i], 0, 9)
    else
        postList = redis.call('zrevrangebyscore', KEYS[i], '+inf', from)
    end
    for j = 1, #postList do
        table.insert(totalList, postList[j] .. '-' .. KEYS[i])
    end
end
return totalList

