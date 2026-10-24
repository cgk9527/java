-- 高并发扣减库存(Lua 原子脚本)
-- KEYS[1]: 库存 key, 例如 stock:sku:1
-- ARGV[1]: 扣减数量
-- 返回值:
--   >= 0 : 扣减成功, 返回剩余库存
--   -1   : 库存 key 不存在(需先初始化库存)
--   -2   : 库存不足
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -1
end
local qty = tonumber(ARGV[1])
if stock < qty then
    return -2
end
return redis.call('DECRBY', KEYS[1], qty)