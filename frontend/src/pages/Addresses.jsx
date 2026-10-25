import React, { useEffect, useState } from 'react';
import { Card, List, Button, Space, Tag, Modal, Form, Input, Switch, Popconfirm, message, Empty } from 'antd';
import { getAddresses, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/index.js';

export default function Addresses() {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    getAddresses()
      .then(setAddresses)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    if (editing) {
      await updateAddress(editing.id, { ...values, isDefault: values.isDefault ? 1 : 0 });
      message.success('已更新地址');
    } else {
      await createAddress({ ...values, isDefault: values.isDefault ? 1 : 0 });
      message.success('已新增地址');
    }
    setModalOpen(false);
    load();
  };

  const onSetDefault = async (id) => {
    await setDefaultAddress(id);
    message.success('已设为默认地址');
    load();
  };

  const onDelete = async (id) => {
    await deleteAddress(id);
    message.success('已删除');
    load();
  };

  const fullAddress = (a) => [a.province, a.city, a.district, a.detail].filter(Boolean).join(' ');

  return (
    <div className="page-container">
      <Card
        title="收货地址"
        extra={<Button type="primary" onClick={openCreate}>新增地址</Button>}
      >
        {addresses.length === 0 ? (
          <Empty description="暂无收货地址" />
        ) : (
          <List
            dataSource={addresses}
            loading={loading}
            renderItem={(a) => (
              <List.Item
                actions={[
                  a.isDefault === 1 ? <Tag color="green">默认</Tag> : <Button size="small" onClick={() => onSetDefault(a.id)}>设为默认</Button>,
                  <Button size="small" onClick={() => openEdit(a)}>编辑</Button>,
                  <Popconfirm title="确认删除该地址?" onConfirm={() => onDelete(a.id)}>
                    <Button size="small" danger>删除</Button>
                  </Popconfirm>
                ]}
              >
                <div>
                  <Space>
                    <strong>{a.receiverName}</strong>
                    <span>{a.receiverPhone}</span>
                  </Space>
                  <div style={{ color: '#666', marginTop: 4 }}>{fullAddress(a)}</div>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>

      <Modal
        title={editing ? '编辑地址' : '新增地址'}
        open={modalOpen}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        okText="保存"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="receiverName" label="收货人" rules={[{ required: true, message: '请输入收货人' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="receiverPhone" label="联系电话" rules={[{ required: true, message: '请输入联系电话' }]}>
            <Input />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }}>
            <Form.Item name="province" label="省">
              <Input />
            </Form.Item>
            <Form.Item name="city" label="市">
              <Input />
            </Form.Item>
            <Form.Item name="district" label="区/县">
              <Input />
            </Form.Item>
          </Space>
          <Form.Item name="detail" label="详细地址" rules={[{ required: true, message: '请输入详细地址' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="isDefault" label="设为默认地址" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}