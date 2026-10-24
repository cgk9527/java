import React, { useEffect, useState } from 'react';
import {
  Card, List, Row, Col, Checkbox, InputNumber, Button, Empty,
  Modal, Form, Input, message, Popconfirm, Spin
} from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getCart, updateCart, deleteCartItem, createOrder, clearCart } from '../api/index.js';

export default function Cart() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    getCart()
      .then((list) => {
        setItems(list);
        setSelected(list.map((i) => i.skuId));
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const onChangeQty = async (skuId, qty) => {
    if (qty <= 0) return;
    await updateCart(skuId, qty);
    load();
  };

  const onDelete = async (skuId) => {
    await deleteCartItem(skuId);
    load();
  };

  const onToggle = (skuId, checked) => {
    setSelected((prev) => (checked ? [...prev, skuId] : prev.filter((id) => id !== skuId)));
  };

  const selectedItems = items.filter((i) => selected.includes(i.skuId));
  const totalAmount = selectedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);

  const onSubmit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      await createOrder({
        items: selectedItems.map((i) => ({ skuId: i.skuId, quantity: i.quantity })),
        ...values
      });
      await clearCart();
      message.success('下单成功');
      navigate('/orders');
    } catch (e) {
      // 错误已由拦截器提示
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-container">
      <Card title="购物车">
        <Spin spinning={loading}>
          {items.length === 0 ? (
            <Empty description="购物车为空">
              <Button type="primary" onClick={() => navigate('/')}>去逛逛</Button>
            </Empty>
          ) : (
            <List
              dataSource={items}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Popconfirm title="确认删除该商品?" onConfirm={() => onDelete(item.skuId)}>
                      <Button type="text" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  ]}
                >
                  <Row style={{ width: '100%' }} align="middle" gutter={16}>
                    <Col flex="40px">
                      <Checkbox
                        checked={selected.includes(item.skuId)}
                        onChange={(e) => onToggle(item.skuId, e.target.checked)}
                      />
                    </Col>
                    <Col flex="80px">
                      <img src={item.productImage} alt={item.productName} style={{ width: 60, height: 60, objectFit: 'cover' }} />
                    </Col>
                    <Col flex="auto">
                      <div>{item.productName}</div>
                      <div style={{ color: '#999' }}>{item.specs}</div>
                    </Col>
                    <Col flex="100px">
                      <span className="price">¥{item.price}</span>
                    </Col>
                    <Col flex="140px">
                      <InputNumber
                        min={1}
                        max={item.stock}
                        value={item.quantity}
                        onChange={(v) => onChangeQty(item.skuId, v)}
                      />
                    </Col>
                    <Col flex="100px" style={{ textAlign: 'right' }}>
                      <span className="price">¥{(item.price * item.quantity).toFixed(2)}</span>
                    </Col>
                  </Row>
                </List.Item>
              )}
            />
          )}
        </Spin>

        {items.length > 0 && (
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <span style={{ marginRight: 16 }}>已选 {selectedItems.length} 件</span>
            <span style={{ marginRight: 16 }}>合计：<span className="price" style={{ fontSize: 22 }}>¥{totalAmount.toFixed(2)}</span></span>
            <Button
              type="primary"
              danger
              size="large"
              disabled={selectedItems.length === 0}
              onClick={() => setModalOpen(true)}
            >
              结算
            </Button>
          </div>
        )}
      </Card>

      <Modal
        title="确认订单"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={onSubmit}
        confirmLoading={submitting}
        okText="提交订单"
      >
        <Form form={form} layout="vertical" initialValues={{ receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' }}>
          <Form.Item name="receiverName" label="收货人" rules={[{ required: true, message: '请输入收货人' }]}>
            <Input placeholder="收货人姓名" />
          </Form.Item>
          <Form.Item name="receiverPhone" label="联系电话" rules={[{ required: true, message: '请输入联系电话' }]}>
            <Input placeholder="手机号码" />
          </Form.Item>
          <Form.Item name="receiverAddress" label="收货地址" rules={[{ required: true, message: '请输入收货地址' }]}>
            <Input.TextArea rows={2} placeholder="详细地址" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}