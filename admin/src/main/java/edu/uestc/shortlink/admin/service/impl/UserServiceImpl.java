package edu.uestc.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.uestc.shortlink.admin.common.convention.exception.ClientException;
import edu.uestc.shortlink.admin.common.enums.UserErrorCodeEnum;
import edu.uestc.shortlink.admin.dao.entity.UserDO;
import edu.uestc.shortlink.admin.dao.mapper.UserMapper;
import edu.uestc.shortlink.admin.dto.req.UserRegisterReqDTO;
import edu.uestc.shortlink.admin.dto.req.UserUpdateReqDTO;
import edu.uestc.shortlink.admin.dto.resp.UserRespDTO;
import edu.uestc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static edu.uestc.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static edu.uestc.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static edu.uestc.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户接口实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final UserMapper userMapper;
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户返回实体
     */
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    /**
     * 查询用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public Boolean hasUserName(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (hasUserName(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        try {
            if (lock.tryLock()) {
                int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
                if (inserted < 1) {
                    throw new ClientException(USER_SAVE_ERROR);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                return;
            }
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // TODO 验证当前用户名是否为登录用户
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }
}
