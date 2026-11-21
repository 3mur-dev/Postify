import { Routes, Route } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProfilePage from "./pages/ProfilePage";
import HomePage from "./pages/HomePage";
import AdminUsersPage from "./pages/admin/AdminUsersPage";
import AdminLayout from "./components/layout/adminLayout";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage";
import AdminUsersDetailsPage from "./pages/admin/AdminUsersDetailsPage";

import AppLayout from "./components/layout/AppLayout";
import PostPage from "./pages/PostPage";
import { Toaster } from "sonner";
import AdminRoute from "./components/layout/adminRoute";
import {NotificationsPage} from "./pages/NotificationsPage";
import SearchPage from "./pages/SearchPage";
import DirectMessagesPage from "./pages/DirectMessagesPage";

function App() {
  return (
    <>
<Toaster
  richColors
  position="top-right"
/>
<Routes>

  <Route element={<AppLayout />}>
    <Route path="/" element={<HomePage />} />
    <Route path="/:username/posts/:publicId" element={<PostPage />} />
    <Route path="/profile" element={<ProfilePage />} />
    <Route path="/profile/:username" element={<ProfilePage />} />
    <Route path="/search" element={<SearchPage />} />
    <Route path="/messages" element={<DirectMessagesPage />} />
    <Route
      path="/messages/:conversationPublicId"
      element={<DirectMessagesPage />}
    />
    <Route
    path="/notifications"
    element={<NotificationsPage />}
/>
  </Route>

  <Route element={<AdminRoute />}>
    <Route element={<AdminLayout />}>
      <Route path="/admin" element={<AdminDashboardPage />} />
      <Route path="/admin/users" element={<AdminUsersPage />} />
      <Route path="/admin/users/:publicId" element={<AdminUsersDetailsPage />} />
    </Route>
  </Route>

  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />
</Routes>
    </>
  );
}

export default App;