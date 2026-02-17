export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "");
    const username = (url.searchParams.get("username") || "").trim();

    try {
      if (request.method === "OPTIONS") {
        return new Response(null, { headers: corsHeaders() });
      }

      if (path === "/health") {
        return await health(env);
      }
      if (path !== "/io-users") {
        return json({ error: "Not found" }, 404);
      }

      if (!username) {
        return json({ error: "Missing username" }, 400);
      }

      const ttlSeconds = 90;
      const nowEpoch = Math.floor(Date.now() / 1000);
      const cutoff = nowEpoch - ttlSeconds;
      const normalized = username.toLowerCase();

      if (!env.IO_USERS_DB) {
        return json({ error: "Missing D1 binding IO_USERS_DB" }, 500);
      }

      await ensureSchema(env.IO_USERS_DB);

      if (username !== ".") {
        await env.IO_USERS_DB
          .prepare(`
            INSERT INTO io_active_users (username_lower, username_display, last_seen)
            VALUES (?1, ?2, ?3)
            ON CONFLICT(username_lower) DO UPDATE SET
              username_display = excluded.username_display,
              last_seen = excluded.last_seen
          `)
          .bind(normalized, username, nowEpoch)
          .run();
      }

      await env.IO_USERS_DB
        .prepare("DELETE FROM io_active_users WHERE last_seen < ?1")
        .bind(cutoff)
        .run();

      const result = await env.IO_USERS_DB
        .prepare("SELECT username_display FROM io_active_users WHERE last_seen >= ?1 ORDER BY username_display ASC")
        .bind(cutoff)
        .all();

      const users = (result.results || []).map((row) => row.username_display);

      return json(
        {
          users,
          count: users.length,
          ttlSeconds
        },
        200
      );
    } catch (error) {
      console.error("io-users worker exception", {
        path,
        username,
        message: String(error?.message || error),
        stack: String(error?.stack || "")
      });

      return json(
        {
          error: "Worker runtime error",
          message: String(error?.message || error),
          stack: String(error?.stack || ""),
          path,
          username,
          hint: "Verify D1 binding IO_USERS_DB and run /health to check connectivity."
        },
        500
      );
    }
  }
};

let schemaReady = false;
async function ensureSchema(db) {
  if (schemaReady) return;
  await db
    .prepare(`
      CREATE TABLE IF NOT EXISTS io_active_users (
        username_lower TEXT PRIMARY KEY,
        username_display TEXT NOT NULL,
        last_seen INTEGER NOT NULL
      )
    `)
    .run();
  schemaReady = true;
}

function json(data, status) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...corsHeaders()
    }
  });
}

function corsHeaders() {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "GET, OPTIONS",
    "access-control-allow-headers": "content-type"
  };
}

async function health(env) {
  const now = new Date().toISOString();
  if (!env.IO_USERS_DB) {
    return json(
      {
        ok: false,
        route: "/health",
        dbBound: false,
        timestamp: now,
        error: "Missing D1 binding IO_USERS_DB"
      },
      500
    );
  }

  try {
    const probe = await env.IO_USERS_DB.prepare("SELECT 1 AS ok").first();
    return json(
      {
        ok: true,
        route: "/health",
        dbBound: true,
        dbQueryOk: probe?.ok === 1,
        timestamp: now
      },
      200
    );
  } catch (error) {
    return json(
      {
        ok: false,
        route: "/health",
        dbBound: true,
        timestamp: now,
        error: String(error?.message || error)
      },
      500
    );
  }
}

