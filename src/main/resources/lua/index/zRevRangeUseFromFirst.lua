--
-- Created by IntelliJ IDEA.
-- User: dell
-- Date: 21/03/01
-- Time: 下午 10:51
-- To change this template use File | Settings | File Templates.
--

if (redis.call('exists', KEYS[1]) == 0) then
    return nil
end
local from = tonumber(ARGV[1])
if (from > redis.call('zcard', KEYS[1])) then
    local rank = redis.call('zrevrank', KEYS[1], ARGV[2])
    if (rank ~= nil and rank ~= false) then
        from = rank
    end
end
if (ARGV[4] == '0') then
    return redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1)
end
return redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1, 'WITHSCORES')