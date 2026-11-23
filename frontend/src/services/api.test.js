import { describe, expect, it } from "vitest";
import { configureAuthToken } from "./authToken.js";

describe("API access-token configuration", () => {
  it("adds the token to protected requests", () => {
    const client = { defaults: { headers: { common: {} } } };
    configureAuthToken(client, "access-token");

    expect(client.defaults.headers.common.Authorization).toBe("Bearer access-token");
  });

  it("removes the token after sign-out", () => {
    const client = { defaults: { headers: { common: {} } } };
    configureAuthToken(client, "access-token");
    configureAuthToken(client, null);

    expect(client.defaults.headers.common.Authorization).toBeUndefined();
  });
});
