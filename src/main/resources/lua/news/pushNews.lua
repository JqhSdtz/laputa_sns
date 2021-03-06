--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:53
-- To change this template use File | Settings | File Templates.
--

redis.call('zadd', KEYS[1], tonumber(ARGV[2]), ARGV[3])
local len = redis.call('zcard', KEYS[1])
if (len > tonumber(ARGV[1])) then
    redis.call('zremrangebyrank', KEYS[1], 0, 0)
end
return len