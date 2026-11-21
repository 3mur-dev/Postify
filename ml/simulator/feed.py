from __future__ import annotations

import random

from simulator.posts import SimulatedPost
from simulator.users import SimulatedUser


def calculate_relevance(
        user: SimulatedUser,
        post: SimulatedPost,
) -> float:

    interests = []

    for topic_id in post.topics:

        interest = user.preferences.get(topic_id)

        if interest is not None:
            interests.append(interest)

    if not interests:
        return 0.0

    return sum(interests) / len(interests)


def select_posts_for_session(
        user: SimulatedUser,
        posts: list[SimulatedPost],
        count: int = 20,
        rng: random.Random | None = None,
        candidate_pool_size: int | None = None,
) -> list[SimulatedPost]:
    """
    Build a feed without using future engagement outcomes.

    Mix:
      - strongly relevant posts
      - weakly relevant posts
      - exploratory (zero-preference) posts

    Exploration rate controls how many low-relevance items appear.
    Candidate pool is larger than the final feed when provided.
    """

    rng = rng or random.Random()

    if not posts or count <= 0:
        return []

    pool_size = candidate_pool_size or max(count * 3, count)
    pool_size = min(pool_size, len(posts))

    if pool_size < len(posts):
        candidates = rng.sample(posts, pool_size)
    else:
        candidates = list(posts)

    scored: list[tuple[SimulatedPost, float]] = [
        (post, calculate_relevance(user, post))
        for post in candidates
    ]

    strong = [(p, r) for p, r in scored if r >= 0.55]
    weak = [(p, r) for p, r in scored if 0.0 < r < 0.55]
    exploratory = [p for p, r in scored if r <= 0.0]

    strong.sort(key=lambda item: item[1], reverse=True)
    weak.sort(key=lambda item: item[1], reverse=True)

    # Higher exploration → more weak/exploratory slots.
    explore_fraction = 0.15 + (user.exploration_rate * 0.70)
    explore_slots = max(1, int(round(count * explore_fraction)))
    relevant_slots = max(0, count - explore_slots)

    selected: list[SimulatedPost] = []
    selected_ids: set[int] = set()
    author_counts: dict[int, int] = {}

    def _try_add(post: SimulatedPost) -> bool:
        if post.post_id in selected_ids:
            return False
        # Soft author diversity: allow repeats only if feed is short on options.
        if author_counts.get(post.author_id, 0) >= 3 and len(selected) < count - 1:
            return False
        selected.append(post)
        selected_ids.add(post.post_id)
        author_counts[post.author_id] = author_counts.get(post.author_id, 0) + 1
        return True

    # Stochastic relevant picks weighted toward higher relevance.
    relevant_pool = strong + weak
    if relevant_pool and relevant_slots > 0:
        weights = [max(0.05, relevance) for _, relevance in relevant_pool]
        picks = _weighted_sample_without_replacement(
            relevant_pool,
            weights,
            min(relevant_slots, len(relevant_pool)),
            rng,
        )
        for post, _ in picks:
            _try_add(post)

    # Exploration / weak-relevance fill.
    explore_sources: list[SimulatedPost] = (
        [post for post, _ in weak]
        + exploratory
        + [post for post, _ in strong]
    )
    rng.shuffle(explore_sources)

    for post in explore_sources:
        if len(selected) >= count:
            break
        if len(selected) >= relevant_slots + explore_slots and len(selected) >= count:
            break
        _try_add(post)

    # Final fill if still short.
    if len(selected) < count:
        leftovers = [post for post, _ in scored if post.post_id not in selected_ids]
        rng.shuffle(leftovers)
        for post in leftovers:
            if len(selected) >= count:
                break
            _try_add(post)

    # Content-type soft shuffle: keep stochastic order, not pure relevance rank.
    rng.shuffle(selected)
    return selected[:count]


def _weighted_sample_without_replacement(
        items: list[tuple[SimulatedPost, float]],
        weights: list[float],
        k: int,
        rng: random.Random,
) -> list[tuple[SimulatedPost, float]]:
    """Weighted sampling without replacement (Efraimidis–Spirakis keys)."""

    if k <= 0 or not items:
        return []

    keyed = []
    for item, weight in zip(items, weights):
        w = max(weight, 1e-9)
        key = rng.random() ** (1.0 / w)
        keyed.append((key, item))

    keyed.sort(key=lambda pair: pair[0], reverse=True)
    return [item for _, item in keyed[:k]]
