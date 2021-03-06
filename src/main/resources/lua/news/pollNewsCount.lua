--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:56
-- To change this template use File | Settings | File Templates.
--

local from = redis.call('hget', KEYS[1], ARGV[1])
if (from == nil or from == false) then
    from = nil;
else
    from = tonumber(from)
end
local cnt = 0
for i = 2, #KEYS do
    if (from == nil) then
        local zCard = redis.call('zcard', KEYS[i])
        if (zCard > 10) then zCard = 10 end
        cnt = cnt + zCard
    else cnt = cnt + redis.call('zcount', KEYS[i], from, '+inf')
    end
end
return cnt

