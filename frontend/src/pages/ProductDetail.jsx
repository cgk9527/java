import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Row, Col, Radio, Button, InputNumber, Input, Rate, List, Avatar, Modal, Empty, Spin, message, Space } from 'antd';
import { ShoppingCartOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { getProductDetail, addToCart, getReviews, createReview } from '../api/index.js';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [skuId, setSkuId] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [reviews, setReviews] = useState([]);
  const [reviewTotal, setReviewTotal] = useState(0);
  const [reviewModal, setReviewModal] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewContent, setReviewContent] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const loadReviews = () => {
    getReviews(id, 1, 10)
      .then((res) => {
        setReviews(res.records || []);
        setReviewTotal(res.total || 0);
      })
      .catch(() => {});
  };

  useEffect(() => {
    setLoading(true);
    getProductDetail(id)
      .then((p) => {
        setProduct(p);
        if (p.skus && p.skus.length > 0) {
          setSkuId(p.skus[0].id);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
    loadReviews();
  }, [id]);

  const currentSku = product?.skus?.find((s) => s.id === skuId);

  const handleAdd = async (goCart) => {
    if (!user) {
      message.warning('请先登录');
      navigate('/login');
      return;
    }
    if (!currentSku) {
      message.warning('请选择规格');
      return;
    }
    await addToCart({ skuId: currentSku.id, quantity });
    message.success('已加入购物车');
    if (goCart) {
      navigate('/cart');
    }
  };

  const openReview = () => {
    if (!user) {
      message.warning('请先登录');
      navigate('/login');
      return;
    }
    setReviewRating(5);
    setReviewContent('');
    setReviewModal(true);
  };

  const submitReview = async () => {
    setSubmittingReview(true);
    try {
      await createReview({ productId: Number(id), rating: reviewRating, content: reviewContent });
      message.success('评价成功');
      setReviewModal(false);
      loadReviews();
    } catch (e) {
      // 拦截器已提示
    } finally {
      setSubmittingReview(false);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 60 }}><Spin size="large" /></div>;
  }

  if (!product) {
    return <div className="page-container"><Card>商品不存在或已下架</Card></div>;
  }

  return (
    <div className="page-container">
      <Card>
        <Row gutter={32}>
          <Col xs={24} md={10}>
            <img src={product.mainImage} alt={product.name} style={{ width: '100%', borderRadius: 8 }} />
          </Col>
          <Col xs={24} md={14}>
            <h1 style={{ marginTop: 0 }}>{product.name}</h1>
            <p style={{ color: '#666' }}>{product.subtitle}</p>
            <div style={{ background: '#fff7f2', padding: 16, borderRadius: 8, margin: '16px 0' }}>
              <span style={{ color: '#999', marginRight: 16 }}>价格</span>
              <span className="price" style={{ fontSize: 28 }}>¥{currentSku ? currentSku.price : product.price}</span>
            </div>

            {product.skus?.length > 0 && (
              <div style={{ margin: '16px 0' }}>
                <div style={{ marginBottom: 8 }}>规格</div>
                <Radio.Group value={skuId} onChange={(e) => setSkuId(e.target.value)}>
                  <Space wrap>
                    {product.skus.map((s) => (
                      <Radio.Button key={s.id} value={s.id} disabled={s.stock <= 0}>
                        {s.specs || '默认'}（¥{s.price}）
                      </Radio.Button>
                    ))}
                  </Space>
                </Radio.Group>
              </div>
            )}

            <div style={{ margin: '16px 0' }}>
              <div style={{ marginBottom: 8 }}>数量</div>
              <InputNumber min={1} max={currentSku?.stock || 1} value={quantity} onChange={setQuantity} />
              <span style={{ marginLeft: 12, color: '#999' }}>
                库存 {currentSku ? currentSku.stock : 0} 件
              </span>
            </div>

            <Space>
              <Button
                type="primary"
                danger
                icon={<ShoppingCartOutlined />}
                onClick={() => handleAdd(false)}
              >
                加入购物车
              </Button>
              <Button type="primary" icon={<ThunderboltOutlined />} onClick={() => handleAdd(true)}>
                立即购买
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {product.detail && (
        <Card title="商品详情" style={{ marginTop: 16 }}>
          <div style={{ whiteSpace: 'pre-wrap' }}>{product.detail}</div>
        </Card>
      )}

      <Card
        title={`商品评价（${reviewTotal}）`}
        style={{ marginTop: 16 }}
        extra={<Button type="primary" size="small" onClick={openReview}>写评价</Button>}
      >
        {reviews.length === 0 ? (
          <Empty description="暂无评价" />
        ) : (
          <List
            dataSource={reviews}
            renderItem={(r) => (
              <List.Item>
                <List.Item.Meta
                  avatar={<Avatar>{r.nickname ? r.nickname[0] : '用'}</Avatar>}
                  title={<Rate disabled value={r.rating} />}
                  description={r.content || '-'}
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Modal
        title="写评价"
        open={reviewModal}
        onOk={submitReview}
        onCancel={() => setReviewModal(false)}
        confirmLoading={submittingReview}
        okText="提交评价"
      >
        <div style={{ marginBottom: 12 }}>
          <span style={{ marginRight: 12 }}>评分</span>
          <Rate value={reviewRating} onChange={setReviewRating} />
        </div>
        <Input.TextArea
          rows={4}
          placeholder="分享你的使用体验（选填）"
          value={reviewContent}
          onChange={(e) => setReviewContent(e.target.value)}
        />
      </Modal>
    </div>
  );
}