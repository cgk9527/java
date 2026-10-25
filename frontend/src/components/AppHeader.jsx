import React, { useState, useEffect } from 'react';
import { Layout, Input, Button, Dropdown, Badge, Space, Avatar } from 'antd';
import { ShoppingCartOutlined, UserOutlined, SearchOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { getCartCount } from '../api/index.js';

const { Header } = Layout;

export default function AppHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (user) {
      getCartCount().then(setCount).catch(() => {});
    } else {
      setCount(0);
    }
  }, [user]);

  const onSearch = () => {
    navigate(keyword ? `/?keyword=${encodeURIComponent(keyword)}` : '/');
  };

  const items = [
    { key: 'orders', label: <Link to="/orders">我的订单</Link> },
    ...(user?.role === 1 ? [{ key: 'admin', label: <Link to="/admin">后台管理</Link> }] : []),
    { key: 'logout', label: <span onClick={logout}>退出登录</span> }
  ];

  return (
    <Header style={{ background: '#ff5000', padding: '0 24px', display: 'flex', alignItems: 'center', gap: 24 }}>
      <Link to="/" style={{ color: '#fff', fontSize: 22, fontWeight: 700 }}>易购商城</Link>
      <div style={{ flex: 1, maxWidth: 520 }}>
        <Input.Search
          placeholder="搜索商品"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={onSearch}
          enterButton={<SearchOutlined />}
        />
      </div>
      <Space size="large">
        <Link to="/cart">
          <Badge count={count} size="small">
            <Button type="text" icon={<ShoppingCartOutlined style={{ color: '#fff', fontSize: 22 }} />} />
          </Badge>
        </Link>
        {user ? (
          <Dropdown menu={{ items }} placement="bottomRight">
            <Space style={{ color: '#fff', cursor: 'pointer' }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <span>{user.nickname || user.username}</span>
            </Space>
          </Dropdown>
        ) : (
          <Space>
            <Button type="text" style={{ color: '#fff' }} onClick={() => navigate('/login')}>登录</Button>
            <Button type="primary" ghost onClick={() => navigate('/register')}>注册</Button>
          </Space>
        )}
      </Space>
    </Header>
  );
}