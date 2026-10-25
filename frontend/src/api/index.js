import request from './request.js';

// 认证
export const register = (data) => request.post('/auth/register', data);
export const login = (data) => request.post('/auth/login', data);
export const getMe = () => request.get('/auth/me');

// 分类
export const getCategories = () => request.get('/categories');

// 商品
export const searchProducts = (params) => request.get('/products/search', { params });
export const getHotProducts = (limit = 8) => request.get('/products/hot', { params: { limit } });
export const getProductDetail = (id) => request.get(`/products/${id}`);

// 购物车
export const getCart = () => request.get('/cart');
export const getCartCount = () => request.get('/cart/count');
export const addToCart = (data) => request.post('/cart', data);
export const updateCart = (skuId, quantity) => request.put(`/cart/${skuId}`, null, { params: { quantity } });
export const deleteCartItem = (skuId) => request.delete(`/cart/${skuId}`);
export const clearCart = () => request.delete('/cart');

// 订单
export const createOrder = (data) => request.post('/orders', data);
export const getOrders = () => request.get('/orders');
export const getOrderDetail = (id) => request.get(`/orders/${id}`);
export const getOrderPayStatus = (id) => request.get(`/orders/${id}/pay-status`);
export const payOrder = (id, channel = 'mock') => request.post(`/orders/${id}/pay`, null, { params: { channel } });
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`);

// 后台管理(管理员)
export const getAdminProducts = () => request.get('/admin/products');
export const createAdminProduct = (data) => request.post('/admin/products', data);
export const updateAdminProduct = (id, data) => request.put(`/admin/products/${id}`, data);
export const deleteAdminProduct = (id) => request.delete(`/admin/products/${id}`);
export const updateAdminProductStatus = (id, status) => request.put(`/admin/products/${id}/status`, null, { params: { status } });
export const getAdminSkus = (productId) => request.get(`/admin/products/${productId}/skus`);
export const addAdminSku = (productId, data) => request.post(`/admin/products/${productId}/skus`, data);
export const updateAdminSku = (skuId, data) => request.put(`/admin/skus/${skuId}`, data);
export const deleteAdminSku = (skuId) => request.delete(`/admin/skus/${skuId}`);