import { HttpInterceptorFn } from '@angular/common/http';

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match ? decodeURIComponent(match[3]) : null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api')) {
    let headers = req.headers;

    // 1. Připojení JWT Bearer tokenu z localStorage
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('zebrak_jwt_token') : null;
    if (token && !headers.has('Authorization')) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    // 2. Připojení CSRF tokenu z cookie pro modifikující požadavky (POST, PUT, DELETE, PATCH)
    const isMutatingMethod = !['GET', 'HEAD', 'OPTIONS'].includes(req.method.toUpperCase());
    if (isMutatingMethod) {
      const xsrfToken = getCookie('XSRF-TOKEN');
      if (xsrfToken) {
        if (!headers.has('X-XSRF-TOKEN')) {
          headers = headers.set('X-XSRF-TOKEN', xsrfToken);
        }
        if (!headers.has('X-CSRF-TOKEN')) {
          headers = headers.set('X-CSRF-TOKEN', xsrfToken);
        }
      }
    }

    const cloned = req.clone({
      withCredentials: true,
      headers: headers,
    });
    return next(cloned);
  }

  return next(req);
};
