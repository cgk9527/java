import React from 'react';
import { Card, Button } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function ProductCard({ product }) {
  const navigate = useNavigate();

  return (
    <Card
      hoverable
      cover={
        <img
          alt={product.name}
          src={product.mainImage}
          style={{ height: 220, objectFit: 'cover' }}
          onClick={() => navigate(`/product/${product.id}`)}
        />
      }
      onClick={() => navigate(`/product/${product.id}`)}
    >
      <Card.Meta
        title={<div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{product.name}</div>}
        description={product.subtitle}
      />
      <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span className="price">¥{product.price ?? '-'}</span>
        <span style={{ color: '#999' }}>销量 {product.sales ?? 0}</span>
      </div>
      <Button
        type="primary"
        danger
        icon={<ShoppingCartOutlined />}
        block
        style={{ marginTop: 12 }}
        onClick={(e) => { e.stopPropagation(); navigate(`/product/${product.id}`); }}
      >
        查看详情
      </Button>
    </Card>
  );
}