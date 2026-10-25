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
export const payOrder = (id) => request.post(`/orders/${id}/pay`);
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`);