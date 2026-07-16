package cache

import (
	"cached_proxy/executor"
	"cached_proxy/repo"
	"fmt"
	"time"
)

type InformationService[V any] interface {
	GetInfo(studentID string) (*V, error)
	submitUpdateTask(studentID string)
}

type AbsInfoService[V any] struct {
	checker   StatusChecker[V]
	onUpdater func(studentID string) (value *V, update bool)
	exec      executor.Executor
	repo      repo.KVRepo[string, cacheItem[V]]
}

func (p *AbsInfoService[V]) getData(key string) *cacheItem[V] {
	value, found := p.repo.Get(key)
	if !found {
		return nil
	}
	return &value
}

func (p *AbsInfoService[V]) setData(key string, item *cacheItem[V]) {
	p.repo.Set(key, *item)
}

func hasUsableCache[V any](item *cacheItem[V]) bool {
	return item != nil && !item.updateAt.IsZero()
}

func (p *AbsInfoService[V]) submitUpdateTask(studentID string) {
	item := p.getData(studentID)
	if item == nil {
		item = &cacheItem[V]{}
	}
	item.submitAt = time.Now()
	p.setData(studentID, item)

	p.exec.Submit(func() {
		value, succeed := p.onUpdater(studentID)
		if succeed {
			formerItem := p.getData(studentID)
			formerItem.data = *value
			formerItem.updateAt = time.Now()
			p.setData(studentID, formerItem)
		}
	})
}

func (p *AbsInfoService[V]) GetInfo(studentID string) (*V, error) {
	item := p.getData(studentID)
	var err error

	switch p.checker.StatusOf(item) {
	case Valid:
		return &item.data, nil
	case Expired:
		err = fmt.Errorf("cache expired")
		p.submitUpdateTask(studentID)
	case NotFound:
		err = fmt.Errorf("cache not found")
		p.submitUpdateTask(studentID)
	case Updating:
		err = fmt.Errorf("cache updating")
	}

	if !hasUsableCache(item) {
		return nil, err
	}
	return &item.data, err
}

func NewPublicInformationService[V any](
	executor2 executor.Executor,
	checker StatusChecker[V],
	onUpdater func(studentID string) (value *V, update bool),
) InformationService[V] {
	return &AbsInfoService[V]{
		exec:      executor2,
		checker:   checker,
		onUpdater: onUpdater,
		repo:      repo.NewStaticRepo[string, cacheItem[V]](),
	}
}

func NewPersonalInformationService[V any](
	executor2 executor.Executor,
	checker StatusChecker[V],
	onUpdater func(studentID string) (value *V, update bool),
) InformationService[V] {
	return &AbsInfoService[V]{
		exec:      executor2,
		checker:   checker,
		onUpdater: onUpdater,
		repo:      repo.NewMemRepo[string, cacheItem[V]](),
	}
}
